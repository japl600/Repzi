package com.japl.repzi.core.domain.model

import kotlin.time.Instant

/** The part of an unfinished workout that tells it exists: which routine it is for and when it began. */
data class WorkoutDraftInfo(
    val routineId: Long,
    val startedAt: Instant,
)
