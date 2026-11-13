package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.model.estimatedOneRepMax
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One [ProgressPoint] for each session where the exercise was done, oldest first. Bodyweight sets (weight 0) count:
 * they give a point with reps but no weight figures, so the screen can chart those exercises by reps.
 */
class ObserveExerciseProgressUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<ProgressPoint>> =
        repository.observeSessions().map { sessions ->
            sessions
                .sortedBy { it.startedAt }
                .mapNotNull { session -> session.progressOf(exerciseId) }
        }
}

private fun WorkoutSession.progressOf(exerciseId: Long): ProgressPoint? {
    // The same exercise can appear more than once in a session, so all its sets are pooled.
    val sets = exercises
        .filter { it.exercise.id == exerciseId }
        .flatMap { it.sets }
        .filter { it.reps > 0 }
    if (sets.isEmpty()) return null

    val weighted = sets.filter { it.weightKg > 0 }
    val heaviest: LoggedSet? = weighted.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
    return ProgressPoint(
        date = startedAt,
        maxWeightKg = heaviest?.weightKg ?: 0.0,
        reps = heaviest?.reps ?: sets.maxOf { it.reps },
        volumeKg = weighted.sumOf { it.weightKg * it.reps },
        estimatedOneRepMaxKg = weighted.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: 0.0,
        maxReps = sets.maxOf { it.reps },
    )
}
