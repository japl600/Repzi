package com.japl.repzi.feature.progress

import androidx.lifecycle.SavedStateHandle
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.ObserveExerciseProgressUseCase
import com.japl.repzi.core.domain.usecase.ObserveTrackedExercisesUseCase
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeClock(var current: Instant) : Clock {
        override fun now() = current
    }

    private val bench = Exercise(1, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val squat = Exercise(2, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private fun day(n: Int) = Instant.fromEpochMilliseconds(n * 86_400_000L)

    private fun session(day: Int, exercise: Exercise, kg: Double) = WorkoutSession(
        id = day.toLong(),
        routineName = "Routine",
        startedAt = day(day),
        finishedAt = day(day) + 1.hours,
        exercises = listOf(LoggedExercise(exercise, listOf(LoggedSet(5, kg)))),
    )

    // Today is day 200; bench sessions at days 10 (old), 150, 190 and 199, and one squat session at day 195.
    private val clock = FakeClock(day(200))
    private val sessions = FakeSessionRepository(
        listOf(
            session(10, bench, 50.0),
            session(150, bench, 60.0),
            session(190, bench, 70.0),
            session(199, bench, 80.0),
            session(195, squat, 100.0),
        ),
    )

    private fun createViewModel(exerciseId: Long = NO_EXERCISE) = ProgressViewModel(
        savedStateHandle = SavedStateHandle(mapOf(EXERCISE_ID_ARG to exerciseId)),
        observeTrackedExercises = ObserveTrackedExercisesUseCase(sessions),
        observeProgress = ObserveExerciseProgressUseCase(sessions),
        clock = clock,
    )

    @Test
    fun opensOnTheRequestedExercise() = runTest {
        val state = createViewModel(squat.id).uiState.first { !it.isLoading }

        assertEquals(squat, state.selected)
        assertEquals(listOf(ProgressPoint(day(195), 100.0, 5)), state.points)
    }

    @Test
    fun withoutAnExerciseItOpensTheFirstOneByName() = runTest {
        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(listOf(bench, squat), state.exercises)
        assertEquals(bench, state.selected)
    }

    @Test
    fun anExerciseWithoutProgressFallsBackToTheFirstOne() = runTest {
        val state = createViewModel(exerciseId = 99).uiState.first { !it.isLoading }

        assertEquals(bench, state.selected)
    }

    @Test
    fun withoutHistoryThereIsNothingSelected() = runTest {
        val empty = FakeSessionRepository()
        val viewModel = ProgressViewModel(
            savedStateHandle = SavedStateHandle(),
            observeTrackedExercises = ObserveTrackedExercisesUseCase(empty),
            observeProgress = ObserveExerciseProgressUseCase(empty),
            clock = clock,
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.selected)
        assertFalse(state.isLoading)
    }

    @Test
    fun theWholeHistoryIsShownByDefaultOldestFirst() = runTest {
        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(ProgressRange.ALL, state.range)
        assertEquals(listOf(50.0, 60.0, 70.0, 80.0), state.points.map { it.maxWeightKg })
    }

    @Test
    fun theRangeKeepsOnlyTheRecentSessions() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onRangeSelected(ProgressRange.MONTH)
        assertEquals(listOf(70.0, 80.0), viewModel.uiState.value.points.map { it.maxWeightKg })

        viewModel.onRangeSelected(ProgressRange.HALF_YEAR)
        assertEquals(listOf(60.0, 70.0, 80.0), viewModel.uiState.value.points.map { it.maxWeightKg })
    }

    @Test
    fun switchingTheExerciseChangesThePoints() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onExerciseSelected(squat.id)

        assertEquals(squat, viewModel.uiState.value.selected)
        assertEquals(listOf(100.0), viewModel.uiState.value.points.map { it.maxWeightKg })
    }

    @Test
    fun recordAndChangeAreComputedOverTheRange() = runTest {
        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(80.0, state.record)
        assertEquals(30.0, state.changeKg)
        assertEquals(60, state.changePercent)
    }

    @Test
    fun oneSessionHasNoChange() = runTest {
        val state = createViewModel(squat.id).uiState.first { !it.isLoading }

        assertNull(state.changeKg)
        assertNull(state.changePercent)
    }
}
