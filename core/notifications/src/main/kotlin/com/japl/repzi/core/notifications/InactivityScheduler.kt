package com.japl.repzi.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val INACTIVITY_WORK_NAME = "inactivity-check"

private const val RUN_HOUR = 18

/**
 * One unique periodic work, once a day, first run at the next 18:00 local time. `KEEP` leaves an existing schedule
 * alone, so calling this on every app start is cheap. It is scheduled whether or not the reminder is on: the worker
 * reads the setting when it runs.
 */
internal class InactivityScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<InactivityCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNextRun(ZonedDateTime.now()))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(INACTIVITY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

/** Time until the next [hour]:00 in the zone of [now], counted on the real clock so a clock change does not shift it. */
internal fun delayUntilNextRun(now: ZonedDateTime, hour: Int = RUN_HOUR): Duration {
    val today = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
    val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, 0).atZone(now.zone)
    return Duration.between(now, next)
}
