package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ejercicios ordenados por nombre, con filtro opcional por grupo muscular. */
class ObserveExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    operator fun invoke(muscleGroup: MuscleGroup? = null): Flow<List<Exercise>> =
        repository.observeExercises(muscleGroup).map { exercises ->
            exercises.sortedBy { it.name.lowercase() }
        }
}
