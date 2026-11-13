package com.japl.repzi.core.domain.model

/** The rest durations an exercise can be given: from 10 seconds to 3 minutes, in steps of 10 seconds. */
object RestTimer {
    const val MIN_SECONDS = 10
    const val MAX_SECONDS = 180
    const val STEP_SECONDS = 10

    val options: List<Int> = (MIN_SECONDS..MAX_SECONDS step STEP_SECONDS).toList()

    /** Core work is usually done with short pauses, everything else starts at a minute. */
    fun defaultFor(group: MuscleGroup): Int = if (group == MuscleGroup.CORE) 20 else 60
}
