package com.japl.repzi.feature.routines

import app.cash.turbine.test
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.PredefinedRoutines
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.RoutineExercise
import com.japl.repzi.core.domain.usecase.AddPredefinedRoutineUseCase
import com.japl.repzi.core.domain.usecase.DeleteRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import com.japl.repzi.core.domain.usecase.SaveRoutineUseCase
import com.japl.repzi.core.testing.FakeExerciseRepository
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val push = Routine(1, "push", listOf(RoutineExercise(squat)))
    private val back = Routine(2, "Back", listOf(RoutineExercise(squat), RoutineExercise(squat.copy(id = 2))))

    private val repository = FakeRoutineRepository(listOf(push, back))

    // The built-in catalog, with a seed key on each exercise as the database stores them.
    private val catalog = listOf(
        Exercise(21, "Sentadilla", MuscleGroup.LEGS, isCustom = false, seedKey = "squat"),
        Exercise(22, "Press de banca", MuscleGroup.CHEST, isCustom = false, seedKey = "bench_press"),
        Exercise(23, "Remo con barra", MuscleGroup.BACK, isCustom = false, seedKey = "barbell_row"),
        Exercise(24, "Press militar", MuscleGroup.SHOULDERS, isCustom = false, seedKey = "overhead_press"),
        Exercise(25, "Plancha", MuscleGroup.CORE, isCustom = false, seedKey = "plank"),
    )

    private val fullBody = PredefinedRoutines.all.first { it.key == "full_body" }

    private fun createViewModel(exercises: List<Exercise> = catalog) = RoutinesViewModel(
        observeRoutines = ObserveRoutinesUseCase(repository),
        deleteRoutine = DeleteRoutineUseCase(repository),
        addPredefinedRoutine = AddPredefinedRoutineUseCase(
            exercises = FakeExerciseRepository(exercises),
            routines = repository,
            saveRoutine = SaveRoutineUseCase(repository),
        ),
    )

    @Test
    fun listsRoutinesSortedByName() = runTest {
        val state = createViewModel().uiState.first { !it.isLoading }

        assertEquals(listOf("Back", "push"), state.routines.map { it.name })
        assertNull(state.routineToDelete)
    }

    @Test
    fun confirmingTheDeletionRemovesTheRoutine() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onDeleteRequested(push)
        assertEquals(push, viewModel.uiState.value.routineToDelete)

        viewModel.onDeleteConfirmed()

        assertNull(viewModel.uiState.value.routineToDelete)
        assertEquals(listOf("Back"), viewModel.uiState.value.routines.map { it.name })
    }

    @Test
    fun dismissingTheDialogKeepsTheRoutine() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onDeleteRequested(push)
        viewModel.onDeleteDismissed()

        assertNull(viewModel.uiState.value.routineToDelete)
        assertEquals(2, viewModel.uiState.value.routines.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun addingAPredefinedRoutineSavesItAndReportsItsId() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.events.test {
            viewModel.onAddPredefined(fullBody, "Cuerpo completo")

            val event = awaitItem() as RoutinesEvent.PredefinedAdded
            assertEquals("Cuerpo completo", event.name)
            val saved = repository.getRoutine(event.id)!!
            assertEquals(listOf(21L, 22L, 23L, 24L, 25L), saved.exercises.map { it.exercise.id })
        }
        assertEquals(3, viewModel.uiState.value.routines.size)
    }

    @Test
    fun addingItTwiceReportsTheNumberedName() = runTest {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAddPredefined(fullBody, "Full body")
            viewModel.onAddPredefined(fullBody, "Full body")

            assertEquals("Full body", (awaitItem() as RoutinesEvent.PredefinedAdded).name)
            assertEquals("Full body (2)", (awaitItem() as RoutinesEvent.PredefinedAdded).name)
        }
    }

    @Test
    fun aCatalogWithoutTheExercisesReportsItAndSavesNothing() = runTest {
        val viewModel = createViewModel(exercises = emptyList())

        viewModel.events.test {
            viewModel.onAddPredefined(fullBody, "Full body")

            assertEquals(RoutinesEvent.PredefinedUnavailable, awaitItem())
        }
        assertEquals(2, repository.observeRoutines().first().size)
    }
}
