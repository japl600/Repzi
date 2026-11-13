package com.japl.repzi.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.usecase.ObserveExercisesUseCase
import com.japl.repzi.core.domain.usecase.ObserveTrackedExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    observeExercises: ObserveExercisesUseCase,
    observeTrackedExercises: ObserveTrackedExercisesUseCase,
) : ViewModel() {

    private val selectedGroup = MutableStateFlow<MuscleGroup?>(null)

    private val exercisesWithProgress: Flow<Set<Long>> =
        observeTrackedExercises().map { tracked -> tracked.mapTo(HashSet()) { it.id } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExercisesUiState> = selectedGroup
        .flatMapLatest { group ->
            combine(observeExercises(group), exercisesWithProgress) { exercises, withProgress ->
                ExercisesUiState(
                    isLoading = false,
                    exercises = exercises,
                    selectedGroup = group,
                    exercisesWithProgress = withProgress,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExercisesUiState(),
        )

    fun onGroupSelected(group: MuscleGroup?) {
        selectedGroup.value = group
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
