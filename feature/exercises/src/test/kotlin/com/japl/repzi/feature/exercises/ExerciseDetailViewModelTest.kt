package com.japl.repzi.feature.exercises

import androidx.lifecycle.SavedStateHandle
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.usecase.ObserveExerciseDetailUseCase
import com.japl.repzi.core.testing.FakeExerciseRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class ExerciseDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository()

    private fun createViewModel(id: Long) = ExerciseDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(EXERCISE_ID_ARG to id)),
        observeDetail = ObserveExerciseDetailUseCase(repository),
    )

    @Test
    fun loadsTheExerciseFromTheNavigationArgument() = runTest {
        val detail = ExerciseDetail(
            id = 2, name = "Plank", muscleGroup = MuscleGroup.CORE, isCustom = false, description = "<p>Hold.</p>",
            author = "wger.de", licenseName = "CC0", licenseUrl = "https://cc0", sourceUrl = "https://wger.de/en/exercise/458/view/",
        )
        repository.setDetails(listOf(detail))

        val state = createViewModel(2).uiState.first { !it.isLoading }

        assertEquals(detail, state.detail)
    }

    @Test
    fun anExerciseThatDoesNotExistHasNoDetail() = runTest {
        val state = createViewModel(404).uiState.first { !it.isLoading }

        assertFalse(state.isLoading)
        assertNull(state.detail)
    }
}
