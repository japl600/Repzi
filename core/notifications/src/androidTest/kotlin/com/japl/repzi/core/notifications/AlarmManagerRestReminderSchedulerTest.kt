package com.japl.repzi.core.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.After
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmManagerRestReminderSchedulerTest {

    private val context = NotificationTestSupport.context
    private val scheduler = AlarmManagerRestReminderScheduler(context)

    @After
    fun tearDown() = scheduler.cancel()

    private fun alarmIsPending(): Boolean {
        val intent = Intent(context, RestReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            AlarmManagerRestReminderScheduler.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) != null
    }

    @Test
    fun scheduleLeavesAPendingAlarm() {
        scheduler.schedule(Clock.System.now() + 1.hours)

        assertTrue(alarmIsPending())
    }

    @Test
    fun cancelRemovesThePendingAlarm() {
        scheduler.schedule(Clock.System.now() + 1.hours)

        scheduler.cancel()

        assertFalse(alarmIsPending())
    }

    @Test
    fun schedulingAgainReplacesTheAlarm() {
        scheduler.schedule(Clock.System.now() + 1.hours)
        scheduler.schedule(Clock.System.now() + 2.hours)

        // One cancel is enough: the second schedule replaced the first instead of adding another alarm.
        scheduler.cancel()

        assertFalse(alarmIsPending())
    }
}
