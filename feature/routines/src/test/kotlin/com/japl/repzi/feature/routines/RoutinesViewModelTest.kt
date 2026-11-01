package com.japl.repzi.feature.routines

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.usecase.DeleteRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
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
    private val push = Routine(1, "push", listOf(squat))
    private val back = Routine(2, "Back", listOf(squat, squat.copy(id = 2)))

    private val repository = FakeRoutineRepository(listOf(push, back))

    private fun createViewModel() = RoutinesViewModel(
        observeRoutines = ObserveRoutinesUseCase(repository),
        deleteRoutine = DeleteRoutineUseCase(repository),
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
}
