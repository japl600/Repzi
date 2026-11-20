package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(muscleGroup: MuscleGroup? = null): Flow<List<Exercise>>

    /** Emits null when there is no exercise with that id. */
    fun observeDetail(id: Long): Flow<ExerciseDetail?>

    suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup)
}
