package com.japl.repzi.feature.progress

import androidx.lifecycle.SavedStateHandle
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
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
        assertEquals(listOf(100.0), state.points.map { it.maxWeightKg })
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
        assertEquals(30.0, state.change)
        assertEquals(60, state.changePercent)
    }

    // Pull-ups: day 198 bodyweight only, day 199 with 10 kg added.
    private val pullUp = Exercise(3, "Dominadas", MuscleGroup.BACK, isCustom = false)
    private val mixed = FakeSessionRepository(
        listOf(
            WorkoutSession(
                id = 1,
                routineName = "Routine",
                startedAt = day(198),
                finishedAt = day(198) + 1.hours,
                exercises = listOf(LoggedExercise(bench, listOf(LoggedSet(5, 60.0), LoggedSet(5, 60.0))), LoggedExercise(pullUp, listOf(LoggedSet(8, 0.0)))),
            ),
            WorkoutSession(
                id = 2,
                routineName = "Routine",
                startedAt = day(199),
                finishedAt = day(199) + 1.hours,
                exercises = listOf(LoggedExercise(bench, listOf(LoggedSet(5, 65.0))), LoggedExercise(pullUp, listOf(LoggedSet(6, 10.0)))),
            ),
        ),
    )

    private fun createMixedViewModel(exerciseId: Long) = ProgressViewModel(
        savedStateHandle = SavedStateHandle(mapOf(EXERCISE_ID_ARG to exerciseId)),
        observeTrackedExercises = ObserveTrackedExercisesUseCase(mixed),
        observeProgress = ObserveExerciseProgressUseCase(mixed),
        clock = clock,
    )

    @Test
    fun theMaxWeightIsPlottedByDefault() = runTest {
        val state = createMixedViewModel(bench.id).uiState.first { !it.isLoading }

        assertEquals(ProgressMetric.MAX_WEIGHT, state.metric)
        assertEquals(ProgressMetric.entries, state.availableMetrics)
    }

    @Test
    fun theMetricChangesWhatIsPlotted() = runTest {
        val viewModel = createMixedViewModel(bench.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onMetricSelected(ProgressMetric.VOLUME)
        // 600 kg on day 198 (two sets of 5 x 60) and 325 kg on day 199.
        assertEquals(listOf(600.0, 325.0), viewModel.uiState.value.series.map { it.value })
        assertEquals(-275.0, viewModel.uiState.value.change)

        viewModel.onMetricSelected(ProgressMetric.REPS)
        assertEquals(listOf(5.0, 5.0), viewModel.uiState.value.series.map { it.value })
    }

    @Test
    fun theOneRepMaxIsEstimatedPerSession() = runTest {
        val viewModel = createMixedViewModel(bench.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onMetricSelected(ProgressMetric.ESTIMATED_ONE_REP_MAX)

        // 60 x 5 gives 70 and 65 x 5 gives 75.8333...
        assertEquals(70.0, viewModel.uiState.value.series.first().value, absoluteTolerance = 1e-9)
        assertEquals(65 * (1 + 5 / 30.0), viewModel.uiState.value.record!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun anExerciseDoneWithBodyweightOnlyIsPlottedByReps() = runTest {
        val repository = FakeSessionRepository(
            listOf(
                WorkoutSession(
                    id = 1,
                    routineName = "Routine",
                    startedAt = day(198),
                    finishedAt = day(198) + 1.hours,
                    exercises = listOf(
                        LoggedExercise(bench, listOf(LoggedSet(5, 60.0))),
                        LoggedExercise(pullUp, listOf(LoggedSet(8, 0.0), LoggedSet(6, 0.0))),
                    ),
                ),
            ),
        )
        val viewModel = ProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf(EXERCISE_ID_ARG to pullUp.id)),
            observeTrackedExercises = ObserveTrackedExercisesUseCase(repository),
            observeProgress = ObserveExerciseProgressUseCase(repository),
            clock = clock,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        // The weight metrics have no data for pull-ups, so only reps is on offer and it is the one plotted.
        assertEquals(listOf(ProgressMetric.REPS), viewModel.uiState.value.availableMetrics)
        assertEquals(ProgressMetric.REPS, viewModel.uiState.value.metric)
        assertEquals(listOf(8.0), viewModel.uiState.value.series.map { it.value })

        // Going back to a weighted exercise restores the default metric.
        viewModel.onExerciseSelected(bench.id)
        assertEquals(ProgressMetric.MAX_WEIGHT, viewModel.uiState.value.metric)
    }

    @Test
    fun sessionsWithoutWeightAreLeftOutOfTheWeightMetrics() = runTest {
        val viewModel = createMixedViewModel(pullUp.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        // Day 198 was bodyweight only; day 199 added 10 kg. Both count as sessions for reps.
        assertEquals(listOf(10.0), viewModel.uiState.value.series.map { it.value })
        viewModel.onMetricSelected(ProgressMetric.REPS)
        assertEquals(listOf(8.0, 6.0), viewModel.uiState.value.series.map { it.value })
    }

    @Test
    fun theChosenMetricSurvivesSwitchingExercises() = runTest {
        val viewModel = createMixedViewModel(bench.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        viewModel.onMetricSelected(ProgressMetric.VOLUME)

        viewModel.onExerciseSelected(pullUp.id)

        assertEquals(ProgressMetric.VOLUME, viewModel.uiState.value.metric)
    }


    @Test
    fun oneSessionHasNoChange() = runTest {
        val state = createViewModel(squat.id).uiState.first { !it.isLoading }

        assertNull(state.change)
        assertNull(state.changePercent)
    }
}
