package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.ObserveExercisesUseCase
import com.japl.repzi.core.domain.usecase.ObserveTrackedExercisesUseCase
import com.japl.repzi.core.testing.FakeExerciseRepository
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false),
            Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false),
        ),
    )

    private val sessions = FakeSessionRepository()

    private fun createViewModel() = ExercisesViewModel(
        observeExercises = ObserveExercisesUseCase(repository),
        observeTrackedExercises = ObserveTrackedExercisesUseCase(sessions),
    )

    @Test
    fun loadsAllExercisesByDefault() = runTest {
        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(listOf("Press de banca", "Sentadilla"), state.exercises.map { it.name })
        assertNull(state.selectedGroup)
    }

    @Test
    fun selectingAGroupFiltersTheList() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onGroupSelected(MuscleGroup.LEGS)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(MuscleGroup.LEGS, state.selectedGroup)
        assertEquals(listOf("Sentadilla"), state.exercises.map { it.name })
    }

    @Test
    fun marksTheExercisesThatHaveLoggedWeight() = runTest {
        val squat = repository.observeExercises().first().first { it.name == "Sentadilla" }
        val bench = repository.observeExercises().first().first { it.name == "Press de banca" }
        sessions.saveSession(
            WorkoutSession(
                routineName = "Full body",
                startedAt = Instant.fromEpochMilliseconds(1_000),
                finishedAt = Instant.fromEpochMilliseconds(2_000),
                exercises = listOf(
                    LoggedExercise(squat, listOf(LoggedSet(5, 100.0))),
                    // Without weight there is no chart, so this one gets no button.
                    LoggedExercise(bench, listOf(LoggedSet(10, 0.0))),
                ),
            ),
        )

        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(setOf(squat.id), state.exercisesWithProgress)
    }
}
