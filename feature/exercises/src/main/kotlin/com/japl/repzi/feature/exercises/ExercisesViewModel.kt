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
import kotlinx.coroutines.flow.asStateFlow
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
    private val query = MutableStateFlow("")

    /**
     * What is typed in the search field. It is exposed apart from [uiState] on purpose: the text
     * field has to read it synchronously, and a value that comes back through the filtering flow
     * arrives a frame late and makes the cursor jump while typing.
     */
    val searchQuery: StateFlow<String> = query.asStateFlow()

    private val exercisesWithProgress: Flow<Set<Long>> =
        observeTrackedExercises().map { tracked -> tracked.mapTo(HashSet()) { it.id } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExercisesUiState> = selectedGroup
        .flatMapLatest { group ->
            combine(observeExercises(group), query, exercisesWithProgress) { exercises, query, withProgress ->
                ExercisesUiState(
                    isLoading = false,
                    exercises = exercises.filter { it.matchesSearch(query) },
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

    fun onSearchQueryChange(text: String) {
        query.value = text
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
