package com.japl.repzi.core.data.sync

import com.japl.repzi.core.domain.model.MuscleGroup

/**
 * The muscle group of a wger category, or null when the exercise does not belong in the app: Cardio (the app logs sets,
 * reps and weight) and any category wger adds later, until it is mapped here on purpose.
 */
internal fun muscleGroupOf(category: String): MuscleGroup? = when (category) {
    "Abs" -> MuscleGroup.CORE
    "Arms" -> MuscleGroup.ARMS
    "Back" -> MuscleGroup.BACK
    "Calves", "Legs" -> MuscleGroup.LEGS
    "Chest" -> MuscleGroup.CHEST
    "Shoulders" -> MuscleGroup.SHOULDERS
    else -> null
}
