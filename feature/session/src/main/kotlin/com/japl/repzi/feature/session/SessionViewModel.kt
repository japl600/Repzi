package com.japl.repzi.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.RestReminderScheduler
import com.japl.repzi.core.domain.usecase.DeleteWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.SaveSessionResult
import com.japl.repzi.core.domain.usecase.SaveSessionUseCase
import com.japl.repzi.core.domain.usecase.SaveWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.SetExerciseRestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoutine: GetRoutineUseCase,
    private val saveSession: SaveSessionUseCase,
    private val getDraft: GetWorkoutDraftUseCase,
    private val saveDraft: SaveWorkoutDraftUseCase,
    private val deleteDraft: DeleteWorkoutDraftUseCase,
    private val setExerciseRest: SetExerciseRestUseCase,
    private val observeRestVibration: ObserveRestVibrationUseCase,
    private val observeRestNotification: ObserveRestNotificationUseCase,
    private val restReminders: RestReminderScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val routineId: Long = checkNotNull(savedStateHandle[ROUTINE_ID_ARG])

    // Replaced by the start time of the draft when an unfinished workout is resumed.
    private var startedAt: Instant = clock.now()

    private val _state = MutableStateFlow(SessionState())

    // Exposed directly (no combine/stateIn hop) so the text fields never receive a stale value while typing.
    val state: StateFlow<SessionState> = _state.asStateFlow()

    // A buffered channel keeps an effect sent while the screen is not collecting until it is back.
    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)
    val effects: Flow<SessionEffect> = _effects.receiveAsFlow()

    private var draftJob: Job? = null

    // The rest in progress is kept as the moment it ends, not as a number that counts down, so it stays right
    // however late the ticks arrive.
    private var restJob: Job? = null
    private var restEndsAt: Instant? = null
    private var restTotalSeconds = 0

    init {
        viewModelScope.launch {
            val routine = getRoutine(routineId)
            if (routine == null) {
                _effects.send(SessionEffect.Close)
                return@launch
            }
            val draft = getDraft(routineId)
            if (draft != null) startedAt = draft.startedAt
            _state.update {
                it.copy(
                    isLoading = false,
                    routineName = routine.name,
                    exercises = routine.exercises.toLogs(draft),
                )
            }
            keepDraftUpToDate()
        }
    }

    fun onIntent(intent: SessionIntent) {
        when (intent) {
            SessionIntent.FinishClicked -> finish()
            SessionIntent.BackClicked -> {
                if (_state.value.hasInput) _state.update { it.reduce(intent) } else close()
            }
            SessionIntent.DiscardConfirmed -> discard()
            is SessionIntent.SetDoneToggled -> toggleDone(intent)
            is SessionIntent.RestChanged -> changeRest(intent)
            is SessionIntent.RestAdjusted -> adjustRest(intent.deltaSeconds)
            SessionIntent.RestSkipped -> stopRest()
            else -> _state.update { it.reduce(intent) }
        }
    }

    /**
     * Mirrors what the user typed into Room after every change, so the workout survives the app being closed. Writes
     * are sequential and skip the values that were replaced while one was in flight, so fast typing costs one write
     * at a time.
     */
    private fun keepDraftUpToDate() {
        draftJob = viewModelScope.launch {
            _state.distinctUntilChangedBy { it.exercises }.collect { state ->
                // A draft is a convenience: failing to write it must never break the workout being logged.
                try {
                    if (state.hasInput) {
                        saveDraft(state.exercises.toDraft(routineId, startedAt))
                    } else {
                        deleteDraft(routineId)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Best effort.
                }
            }
        }
    }

    /** Stops the writer first, otherwise a write still in flight could bring the deleted draft back. */
    private suspend fun discardDraft() {
        draftJob?.cancelAndJoin()
        deleteDraft(routineId)
    }

    private fun finish() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val session = current.toWorkoutSession(startedAt, clock.now())
            when (saveSession(session)) {
                // Two separate writes: if the process dies in between, the draft outlives its session and the
                // workout can be finished again. That is preferred over losing it.
                is SaveSessionResult.Saved -> {
                    restReminders.cancel()
                    discardDraft()
                    _effects.send(SessionEffect.Close)
                }
                SaveSessionResult.NothingLogged ->
                    _state.update { it.copy(isSaving = false, error = SessionError.NothingLogged) }
            }
        }
    }

    private fun discard() {
        viewModelScope.launch {
            restReminders.cancel()
            discardDraft()
            _effects.send(SessionEffect.Close)
        }
    }

    private fun close() {
        restReminders.cancel()
        viewModelScope.launch { _effects.send(SessionEffect.Close) }
    }

    /** Ticking a set starts the rest of its exercise, if it has one; unticking only changes the tick. */
    private fun toggleDone(intent: SessionIntent.SetDoneToggled) {
        val log = _state.value.exercises.getOrNull(intent.exerciseIndex) ?: return
        val wasDone = log.sets.getOrNull(intent.setIndex)?.done ?: return
        _state.update { it.reduce(intent) }
        if (!wasDone) log.restSeconds?.let(::startRest)
    }

    /** Applies the new rest on screen and keeps it in the routine, so the next workout starts with it. */
    private fun changeRest(intent: SessionIntent.RestChanged) {
        if (intent.exerciseIndex !in _state.value.exercises.indices) return
        _state.update { it.reduce(intent) }
        viewModelScope.launch {
            // Remembering the rest is a convenience: failing to store it must not interrupt the workout.
            try {
                setExerciseRest(routineId, intent.exerciseIndex, intent.seconds)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best effort.
            }
        }
    }

    private fun startRest(seconds: Int) {
        restJob?.cancel()
        restTotalSeconds = seconds
        val endsAt = clock.now() + seconds.seconds
        restEndsAt = endsAt
        scheduleReminder(endsAt)
        restJob = viewModelScope.launch { tickRest() }
    }

    /**
     * Asks for the alarm that wakes the user when the rest ends with the app out of sight. The setting is read here, so
     * the user's latest choice applies, and the alarm is only set if this rest is still the one in progress by then.
     * It is cancelled by what the user does (skip, finish, leave), never at the natural end of the countdown: the
     * countdown also reaches zero in the background, and the alarm decides by itself whether the app is on screen.
     */
    private fun scheduleReminder(endsAt: Instant) {
        viewModelScope.launch {
            if (observeRestNotification().first() && restEndsAt == endsAt) restReminders.schedule(endsAt)
        }
    }

    private suspend fun tickRest() {
        while (true) {
            val endsAt = restEndsAt ?: return
            val remaining = secondsUntil(endsAt)
            if (remaining <= 0) {
                clearRest()
                // Read now, not when the countdown started, so a change made while it was running applies.
                if (observeRestVibration().first()) {
                    _effects.send(SessionEffect.RestFinished)
                }
                return
            }
            _state.update { it.copy(rest = RestCountdown(restTotalSeconds, remaining)) }
            delay(TICK)
        }
    }

    /** Moves the end of the rest in progress. Taking away everything that was left ends it without a buzz. */
    private fun adjustRest(deltaSeconds: Int) {
        val endsAt = restEndsAt ?: return
        val newEnd = endsAt + deltaSeconds.seconds
        val remaining = secondsUntil(newEnd)
        if (remaining <= 0) {
            stopRest()
            return
        }
        restEndsAt = newEnd
        scheduleReminder(newEnd)
        restTotalSeconds = (restTotalSeconds + deltaSeconds).coerceAtLeast(remaining)
        _state.update { it.copy(rest = RestCountdown(restTotalSeconds, remaining)) }
    }

    private fun stopRest() {
        restJob?.cancel()
        restReminders.cancel()
        clearRest()
    }

    private fun clearRest() {
        restEndsAt = null
        _state.update { it.copy(rest = null) }
    }

    /** Whole seconds left, rounded up so the screen shows 0:01 until the rest is really over. */
    private fun secondsUntil(endsAt: Instant): Int {
        val millis = (endsAt - clock.now()).inWholeMilliseconds
        return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    private companion object {
        val TICK = 200.milliseconds
        const val MILLIS_PER_SECOND = 1_000L
    }
}
