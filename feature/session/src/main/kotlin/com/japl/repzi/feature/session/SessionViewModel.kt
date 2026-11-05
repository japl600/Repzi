package com.japl.repzi.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.usecase.DeleteWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.SaveSessionResult
import com.japl.repzi.core.domain.usecase.SaveSessionUseCase
import com.japl.repzi.core.domain.usecase.SaveWorkoutDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
            discardDraft()
            _effects.send(SessionEffect.Close)
        }
    }

    private fun close() {
        viewModelScope.launch { _effects.send(SessionEffect.Close) }
    }
}
