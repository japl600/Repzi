package com.japl.repzi.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.SaveSessionResult
import com.japl.repzi.core.domain.usecase.SaveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getRoutine: GetRoutineUseCase,
    private val saveSession: SaveSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val routineId: Long = checkNotNull(savedStateHandle[ROUTINE_ID_ARG])

    // Stored in the handle so a workout restored after process death keeps its real start time.
    private val startedAt = Instant.fromEpochMilliseconds(
        savedStateHandle.get<Long>(STARTED_AT_KEY)
            ?:clock.now().toEpochMilliseconds().also { savedStateHandle[STARTED_AT_KEY] = it },
    )

    private val _state = MutableStateFlow(SessionState())

    // Exposed directly (no combine/stateIn hop) so the text fields never receive a stale value while typing.
    val state: StateFlow<SessionState> = _state.asStateFlow()

    // A buffered channel keeps an effect sent while the screen is not collecting until it is back.
    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)
    val effects: Flow<SessionEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val routine = getRoutine(routineId)
            if (routine == null) {
                _effects.send(SessionEffect.Close)
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        routineName = routine.name,
                        exercises = routine.exercises.toLogs(
                            savedStateHandle.get<ArrayList<DraftExercise>>(DRAFT_KEY),
                        ),
                    )
                }
            }
        }
        // Mirrors the typed sets into the handle after every change; the system writes it out when the app stops.
        viewModelScope.launch {
            _state.collect { state ->
                if (!state.isLoading) savedStateHandle[DRAFT_KEY] = state.exercises.toDraft()
            }
        }
    }

    fun onIntent(intent: SessionIntent) {
        when (intent) {
            SessionIntent.FinishClicked -> finish()
            SessionIntent.BackClicked -> {
                if (_state.value.hasInput) _state.update { it.reduce(intent) } else close()
            }
            SessionIntent.DiscardConfirmed -> close()
            else -> _state.update { it.reduce(intent) }
        }
    }

    private fun finish() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val session = current.toWorkoutSession(startedAt, clock.now())
            when (saveSession(session)) {
                is SaveSessionResult.Saved -> _effects.send(SessionEffect.Close)
                SaveSessionResult.NothingLogged ->
                    _state.update { it.copy(isSaving = false, error = SessionError.NothingLogged) }
            }
        }
    }

    private fun close() {
        viewModelScope.launch { _effects.send(SessionEffect.Close) }
    }
}
