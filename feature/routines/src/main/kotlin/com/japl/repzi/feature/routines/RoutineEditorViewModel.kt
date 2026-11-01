package com.japl.repzi.feature.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveExercisesUseCase
import com.japl.repzi.core.domain.usecase.SaveRoutineResult
import com.japl.repzi.core.domain.usecase.SaveRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeExercises: ObserveExercisesUseCase,
    private val getRoutine: GetRoutineUseCase,
    private val saveRoutine: SaveRoutineUseCase,
) : ViewModel() {

    // Navigation puts the route argument in the SavedStateHandle.
    private val routineId: Long = savedStateHandle[ROUTINE_ID_ARG] ?: Routine.NEW_ID

    private val _uiState = MutableStateFlow(RoutineEditorUiState(isNew = routineId == Routine.NEW_ID))

    // Exposed directly (no combine/stateIn hop) so the text field never receives a stale value while typing.
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    /** Catalog shown in the exercise picker. */
    val availableExercises: StateFlow<List<Exercise>> = observeExercises().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    init {
        if (routineId == Routine.NEW_ID) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                val routine = getRoutine(routineId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = routine?.name.orEmpty(),
                        exercises = routine?.exercises.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    /** Adds the exercise at the end of the routine, or removes it when it is already part of it. */
    fun onExerciseToggled(exercise: Exercise) {
        _uiState.update { state ->
            val exercises = if (exercise in state.exercises) state.exercises - exercise else state.exercises + exercise
            state.copy(exercises = exercises, error = null)
        }
    }

    fun onExerciseRemoved(index: Int) = reorder { it.apply { removeAt(index) } }

    fun onExerciseMovedUp(index: Int) = move(index, index - 1)

    fun onExerciseMovedDown(index: Int) = move(index, index + 1)

    fun onSaveClicked() {
        val state = _uiState.value
        viewModelScope.launch {
            val result = saveRoutine(Routine(id = routineId, name = state.name, exercises = state.exercises))
            _uiState.update {
                when (result) {
                    is SaveRoutineResult.Saved -> it.copy(isSaved = true)
                    SaveRoutineResult.BlankName -> it.copy(error = RoutineEditorError.BlankName)
                    SaveRoutineResult.NoExercises -> it.copy(error = RoutineEditorError.NoExercises)
                }
            }
        }
    }

    private fun move(from: Int, to: Int) = reorder { list ->
        if (to in list.indices) list.add(to, list.removeAt(from))
        list
    }

    private fun reorder(change: (MutableList<Exercise>) -> MutableList<Exercise>) {
        _uiState.update { it.copy(exercises = change(it.exercises.toMutableList())) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
