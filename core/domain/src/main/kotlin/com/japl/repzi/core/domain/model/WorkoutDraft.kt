package com.japl.repzi.core.domain.model

import kotlin.time.Instant

/**
 * A workout in progress, kept so it survives the app being closed. There is at most one per routine. Weight and reps
 * are the raw text the user typed (not parsed yet), exactly as the session screen holds them.
 */
data class WorkoutDraft(
    val routineId: Long,
    val startedAt: Instant,
    val exercises: List<DraftExercise>,
)

data class DraftExercise(
    val exerciseId: Long,
    val sets: List<DraftSet>,
)

data class DraftSet(
    val weight: String,
    val reps: String,
)
