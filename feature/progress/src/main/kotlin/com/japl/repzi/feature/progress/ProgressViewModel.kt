package com.japl.repzi.feature.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.usecase.ObserveExerciseProgressUseCase
import com.japl.repzi.core.domain.usecase.ObserveTrackedExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTrackedExercises: ObserveTrackedExercisesUseCase,
    private val observeProgress: ObserveExerciseProgressUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val selectedId = MutableStateFlow(
        (savedStateHandle[EXERCISE_ID_ARG] ?: NO_EXERCISE).takeIf { it != NO_EXERCISE },
    )
    private val range = MutableStateFlow(ProgressRange.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> =
        combine(observeTrackedExercises(), selectedId, range) { tracked, id, range ->
            // An id that has no progress (or none at all) falls back to the first exercise of the list.
            Triple(tracked, tracked.firstOrNull { it.id == id } ?: tracked.firstOrNull(), range)
        }
            .flatMapLatest { (tracked, selected, range) ->
                if (selected == null) {
                    flowOf(ProgressUiState(isLoading = false))
                } else {
                    observeProgress(selected.id).map { points ->
                        ProgressUiState(
                            isLoading = false,
                            exercises = tracked,
                            selected = selected,
                            range = range,
                            points = points.within(range, clock.now()),
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProgressUiState(),
            )

    fun onExerciseSelected(id: Long) {
        selectedId.value = id
    }

    fun onRangeSelected(newRange: ProgressRange) {
        range.value = newRange
    }

    private fun List<ProgressPoint>.within(range: ProgressRange, now: Instant): List<ProgressPoint> {
        val days = range.days ?: return this
        val since = now - days.days
        return filter { it.date >= since }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
