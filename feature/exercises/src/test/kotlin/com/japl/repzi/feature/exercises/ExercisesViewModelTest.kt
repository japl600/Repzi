package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.usecase.ObserveExercisesUseCase
import com.japl.repzi.core.testing.FakeExerciseRepository
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
class ExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false),
            Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false),
        ),
    )

    private fun createViewModel() = ExercisesViewModel(ObserveExercisesUseCase(repository))

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
}
