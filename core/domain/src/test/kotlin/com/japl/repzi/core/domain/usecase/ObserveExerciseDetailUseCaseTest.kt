package com.japl.repzi.core.domain.usecase

import app.cash.turbine.test
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ObserveExerciseDetailUseCaseTest {

    private val detail = ExerciseDetail(
        id = 7,
        name = "Kettlebell swing",
        muscleGroup = MuscleGroup.CORE,
        isCustom = false,
        description = "<p>Swing.</p>",
        author = "deusinvictus",
        licenseName = "CC-BY-SA 4",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/deed.en",
        sourceUrl = "https://wger.de/en/exercise/9/view/",
    )

    private val repository = object : ExerciseRepository {
        override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> = flowOf(emptyList())

        override fun observeDetail(id: Long): Flow<ExerciseDetail?> = flowOf(detail.takeIf { it.id == id })

        override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) = Unit
    }

    private val useCase = ObserveExerciseDetailUseCase(repository)

    @Test
    fun emitsTheDetailOfTheExercise() = runTest {
        useCase(7).test {
            assertEquals(detail, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun emitsNullForAnExerciseThatDoesNotExist() = runTest {
        useCase(99).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
