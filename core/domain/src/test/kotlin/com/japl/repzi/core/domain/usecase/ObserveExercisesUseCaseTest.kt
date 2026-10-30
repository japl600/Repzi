package com.japl.repzi.core.domain.usecase

import app.cash.turbine.test
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ObserveExercisesUseCaseTest {

    private val exercises = listOf(
        Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false),
        Exercise(2, "press banca", MuscleGroup.CHEST, isCustom = false),
        Exercise(3, "Curl de biceps", MuscleGroup.ARMS, isCustom = true),
    )

    private val repository = object : ExerciseRepository {
        override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> =
            flowOf(exercises.filter { muscleGroup == null || it.muscleGroup == muscleGroup })

        override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) = Unit
    }

    private val useCase = ObserveExercisesUseCase(repository)

    @Test
    fun ordersByNameIgnoringCase() = runTest {
        useCase().test {
            assertEquals(listOf("Curl de biceps", "press banca", "Sentadilla"), awaitItem().map { it.name })
            awaitComplete()
        }
    }

    @Test
    fun filtersByMuscleGroup() = runTest {
        useCase(MuscleGroup.LEGS).test {
            assertEquals(listOf("Sentadilla"), awaitItem().map { it.name })
            awaitComplete()
        }
    }
}
