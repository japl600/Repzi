package com.japl.repzi.core.domain

import kotlin.time.Instant

/** Wakes the user when a rest countdown ends while the app is not on screen. Features never see how it is scheduled. */
interface RestReminderScheduler {
    /** Replaces any reminder already scheduled: there is at most one rest in progress. */
    fun schedule(endsAt: Instant)

    fun cancel()
}
