package com.japl.repzi.core.domain

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** When the "you stopped training" notice is due. Pure, so the daily check and its tests share one rule. */
object InactivityRule {

    /**
     * The whole days since [lastWorkoutAt] when a notice is due, or null. A notice is due once [days] days have passed
     * and the latest workout is newer than the one the last notice was sent for ([lastNoticedFor]): one notice per lapse,
     * and deleting the latest workout cannot bring back a notice for an older one. Nothing is due before the first workout.
     */
    fun daysToReport(now: Instant, lastWorkoutAt: Instant?, days: Int, lastNoticedFor: Instant?): Int? {
        if (lastWorkoutAt == null) return null
        if (lastNoticedFor != null && lastWorkoutAt <= lastNoticedFor) return null
        val elapsed = now - lastWorkoutAt
        if (elapsed < days.days) return null
        return elapsed.inWholeDays.toInt()
    }
}
