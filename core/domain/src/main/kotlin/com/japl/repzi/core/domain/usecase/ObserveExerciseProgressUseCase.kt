package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Heaviest weight lifted with an exercise in each session, oldest first. Sessions where the exercise was only done
 * without added weight (bodyweight, weight 0) are left out: they have nothing to plot on a weight axis.
 */
class ObserveExerciseProgressUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<ProgressPoint>> =
        repository.observeSessions().map { sessions ->
            sessions
                .sortedBy { it.startedAt }
                .mapNotNull { session -> session.bestSetOf(exerciseId) }
        }
}

private fun WorkoutSession.bestSetOf(exerciseId: Long): ProgressPoint? {
    // The same exercise can appear more than once in a session, so all its sets are pooled.
    val best = exercises
        .filter { it.exercise.id == exerciseId }
        .flatMap { it.sets }
        .filter { it.weightKg > 0 }
        .maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
    return best?.let { ProgressPoint(date = startedAt, maxWeightKg = it.weightKg, reps = it.reps) }
}
