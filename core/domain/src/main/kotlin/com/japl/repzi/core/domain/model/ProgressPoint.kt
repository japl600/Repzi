package com.japl.repzi.core.domain.model

import kotlin.time.Instant

/**
 * The heaviest set of one exercise in one session: [maxWeightKg] and the [reps] done with it (the best of them when
 * several sets share that weight). One point per session, so a list of them is the exercise's progress over time.
 */
data class ProgressPoint(
    val date: Instant,
    val maxWeightKg: Double,
    val reps: Int,
)
