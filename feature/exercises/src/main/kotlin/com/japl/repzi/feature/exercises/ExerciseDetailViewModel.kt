package com.japl.repzi.feature.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.usecase.ObserveExerciseDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveExerciseDetailUseCase,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle[EXERCISE_ID_ARG]) { "Missing $EXERCISE_ID_ARG" }

    val uiState: StateFlow<ExerciseDetailUiState> = observeDetail(exerciseId)
        .map { ExerciseDetailUiState(isLoading = false, detail = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExerciseDetailUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
