package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {

    private val exercises = MutableStateFlow(initial)

    override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> =
        exercises.map { list -> list.filter { muscleGroup == null || it.muscleGroup == muscleGroup } }

    override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) {
        val nextId = (exercises.value.maxOfOrNull { it.id } ?: 0) + 1
        exercises.value += Exercise(nextId, name, muscleGroup, isCustom = true)
    }

    fun setExercises(list: List<Exercise>) {
        exercises.value = list
    }
}
