package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Exercises that have at least one weighted set in the history (the ones with a progress chart), by name. */
class ObserveTrackedExercisesUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    operator fun invoke(): Flow<List<Exercise>> =
        repository.observeSessions().map { sessions ->
            sessions
                .flatMap { it.exercises }
                .filter { logged -> logged.sets.any { it.weightKg > 0 } }
                .map { it.exercise }
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
        }
}
