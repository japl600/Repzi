package com.japl.repzi.feature.routines

import androidx.lifecycle.SavedStateHandle
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveExercisesUseCase
import com.japl.repzi.core.domain.usecase.SaveRoutineUseCase
import com.japl.repzi.core.testing.FakeExerciseRepository
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val row = Exercise(3, "Remo con barra", MuscleGroup.BACK, isCustom = false)

    private val routines = FakeRoutineRepository(listOf(Routine(5, "Full body", listOf(squat, bench, row))))
    private val exercises = FakeExerciseRepository(listOf(squat, bench, row))

    private fun createViewModel(routineId: Long? = null) = RoutineEditorViewModel(
        savedStateHandle = SavedStateHandle(if (routineId == null) emptyMap() else mapOf(ROUTINE_ID_ARG to routineId)),
        observeExercises = ObserveExercisesUseCase(exercises),
        getRoutine = GetRoutineUseCase(routines),
        saveRoutine = SaveRoutineUseCase(routines),
    )

    @Test
    fun newRoutineStartsEmptyAndReady() = runTest {
        val state = createViewModel().uiState.value

        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals(emptyList(), state.exercises)
    }

    @Test
    fun existingRoutineIsLoadedForEditing() = runTest {
        val state = createViewModel(routineId = 5).uiState.value

        assertFalse(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("Full body", state.name)
        assertEquals(listOf(squat, bench, row), state.exercises)
    }

    @Test
    fun togglingAnExerciseAddsAndRemovesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.onExerciseToggled(bench)
        viewModel.onExerciseToggled(squat)
        assertEquals(listOf(bench, squat), viewModel.uiState.value.exercises)

        viewModel.onExerciseToggled(bench)
        assertEquals(listOf(squat), viewModel.uiState.value.exercises)
    }

    @Test
    fun exercisesCanBeReorderedAndRemoved() = runTest {
        val viewModel = createViewModel(routineId = 5)

        viewModel.onExerciseMovedDown(0)
        assertEquals(listOf(bench, squat, row), viewModel.uiState.value.exercises)

        viewModel.onExerciseMovedUp(2)
        assertEquals(listOf(bench, row, squat), viewModel.uiState.value.exercises)

        // Moving past either end is ignored.
        viewModel.onExerciseMovedUp(0)
        viewModel.onExerciseMovedDown(2)
        assertEquals(listOf(bench, row, squat), viewModel.uiState.value.exercises)

        viewModel.onExerciseRemoved(1)
        assertEquals(listOf(bench, squat), viewModel.uiState.value.exercises)
    }

    @Test
    fun savingWithoutNameShowsAnError() = runTest {
        val viewModel = createViewModel()
        viewModel.onExerciseToggled(squat)

        viewModel.onSaveClicked()

        assertEquals(RoutineEditorError.BlankName, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun savingWithoutExercisesShowsAnError() = runTest {
        val viewModel = createViewModel()
        viewModel.onNameChange("Legs")

        viewModel.onSaveClicked()

        assertEquals(RoutineEditorError.NoExercises, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun editingTheFormClearsTheError() = runTest {
        val viewModel = createViewModel()
        viewModel.onSaveClicked()
        assertEquals(RoutineEditorError.BlankName, viewModel.uiState.value.error)

        viewModel.onNameChange("Legs")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun savingAValidRoutinePersistsItAndFlagsCompletion() = runTest {
        val viewModel = createViewModel()
        viewModel.onNameChange("  Legs ")
        viewModel.onExerciseToggled(squat)

        viewModel.onSaveClicked()

        assertTrue(viewModel.uiState.value.isSaved)
        val saved = routines.observeRoutines().first().single { it.name == "Legs" }
        assertEquals(listOf(squat), saved.exercises)
    }

    @Test
    fun savingAnEditedRoutineReplacesTheOriginal() = runTest {
        val viewModel = createViewModel(routineId = 5)
        viewModel.onNameChange("Full body B")
        viewModel.onExerciseRemoved(2)

        viewModel.onSaveClicked()

        val all = routines.observeRoutines().first()
        assertEquals(1, all.size)
        assertEquals(Routine(5, "Full body B", listOf(squat, bench)), all.single())
    }

    @Test
    fun exposesTheCatalogForThePicker() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.availableExercises.collect { } }

        assertEquals(listOf("Press de banca", "Remo con barra", "Sentadilla"), viewModel.availableExercises.value.map { it.name })
    }
}
