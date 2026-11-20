package com.japl.repzi.core.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.japl.repzi.core.domain.RestReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Instant

/**
 * One alarm for the rest in progress. It lives in the system, not in the app, so it fires with the screen off and after
 * the app was swiped away. The alarm is exact when the user allowed exact alarms and approximate otherwise.
 */
internal class AlarmManagerRestReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RestReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // The same request code every time, so scheduling replaces the alarm and cancelling removes it.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RestReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // The permission is declared in the manifest and checked right here, which lint cannot see through a helper.
    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(endsAt: Instant) {
        val triggerAt = endsAt.toEpochMilliseconds()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
        }
    }

    override fun cancel() {
        val pending = pendingIntent()
        alarmManager.cancel(pending)
        pending.cancel()
    }

    internal companion object {
        const val REQUEST_CODE = 1
    }
}
