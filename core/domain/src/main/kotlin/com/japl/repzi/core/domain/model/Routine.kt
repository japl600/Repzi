package com.japl.repzi.core.domain.model

/** A named, ordered list of exercises. An [id] of [NEW_ID] means the routine has not been saved yet. */
data class Routine(
    val id: Long = NEW_ID,
    val name: String,
    val exercises: List<RoutineExercise>,
) {
    companion object {
        const val NEW_ID = 0L
    }
}

/**
 * An exercise inside a routine. [restSeconds] is the rest to take between its sets, or null when the routine does not
 * use a rest timer for it. It belongs to the routine and not to the exercise, so the same exercise can rest for a
 * different time in two routines.
 */
data class RoutineExercise(
    val exercise: Exercise,
    val restSeconds: Int? = null,
)
