package com.japl.repzi.core.domain.model

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A finished workout. [routineName] is a snapshot: the session keeps its title even if the routine it came from is
 * renamed or deleted later.
 */
data class WorkoutSession(
    val id: Long = NEW_ID,
    val routineName: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val exercises: List<LoggedExercise>,
) {
    val duration: Duration get() = finishedAt - startedAt

    /** Total weight lifted: the sum of reps times weight over every set. */
    val totalVolumeKg: Double get() = exercises.sumOf { it.volumeKg }

    companion object {
        const val NEW_ID = 0L
    }
}

/** An exercise performed during a session. The same exercise can appear more than once. */
data class LoggedExercise(
    val exercise: Exercise,
    val sets: List<LoggedSet>,
) {
    val volumeKg: Double get() = sets.sumOf { it.reps * it.weightKg }
}

data class LoggedSet(
    val reps: Int,
    val weightKg: Double,
)
