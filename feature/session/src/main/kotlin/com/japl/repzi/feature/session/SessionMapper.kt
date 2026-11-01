package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.WorkoutSession
import kotlin.time.Instant

/**
 * Parses the typed text into domain sets. A blank weight means bodyweight (0 kg); a set with an unreadable value
 * becomes an empty set (0 reps) that the use case discards.
 */
internal fun SessionState.toWorkoutSession(startedAt: Instant, finishedAt: Instant) = WorkoutSession(
    routineName = routineName,
    startedAt = startedAt,
    finishedAt = finishedAt,
    exercises = exercises.map { log ->
        LoggedExercise(log.exercise, log.sets.map { it.toLoggedSet() })
    },
)

private fun SetInput.toLoggedSet(): LoggedSet {
    val weightKg = if (weight.isBlank()) 0.0 else weight.toDoubleOrNull()
    val repCount = reps.toIntOrNull()
    return if (weightKg == null || repCount == null) LoggedSet(0, 0.0) else LoggedSet(repCount, weightKg)
}
