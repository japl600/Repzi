package com.japl.repzi.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

/**
 * The alarms are counted in the system's own list (`dumpsys alarm`), because a `PendingIntent` can exist without any
 * alarm behind it: checking only that would pass even if the scheduler never called `AlarmManager`.
 */
@RunWith(AndroidJUnit4::class)
class AlarmManagerRestReminderSchedulerTest {

    private val context = NotificationTestSupport.context
    private val scheduler = AlarmManagerRestReminderScheduler(context)

    @Before
    fun setUp() = scheduler.cancel()

    @After
    fun tearDown() = scheduler.cancel()

    private fun registeredAlarms(): Int {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys alarm")
        val dump = ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes().decodeToString() }
        // The pending alarms come first. The sections after them (stats, history of removed alarms) still mention an
        // alarm that was cancelled, so they must not be counted.
        val pending = dump
            .substringBefore("LazyAlarmStore stats:")
            .substringBefore("Removal history:")
            .substringBefore("Alarm Stats:")
        return pending.lines().count { line ->
            line.contains("tag=") && line.contains("${context.packageName}/") && line.contains("RestReminderReceiver")
        }
    }

    @Test
    fun scheduleRegistersAnAlarmInTheSystem() {
        scheduler.schedule(Clock.System.now() + 1.hours)

        assertTrue(registeredAlarms() >= 1)
    }

    @Test
    fun cancelRemovesTheAlarmFromTheSystem() {
        scheduler.schedule(Clock.System.now() + 1.hours)

        scheduler.cancel()

        assertEquals(0, registeredAlarms())
    }

    // The system drops nothing here by itself; cancelling its alarm directly stands in for "the alarm has to be set again".
    private fun dropTheSystemAlarmOnly() {
        val pending = PendingIntent.getBroadcast(
            context,
            AlarmManagerRestReminderScheduler.REQUEST_CODE,
            Intent(context, RestReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        context.getSystemService(AlarmManager::class.java).cancel(pending!!)
    }

    @Test
    fun rescheduleIfPendingSetsTheAlarmAgainForARestStillRunning() {
        scheduler.schedule(Clock.System.now() + 1.hours)
        dropTheSystemAlarmOnly()
        assertEquals(0, registeredAlarms())

        scheduler.rescheduleIfPending()

        assertTrue(registeredAlarms() >= 1)
    }

    @Test
    fun rescheduleIfPendingDoesNothingOnceTheRestWasCancelled() {
        scheduler.schedule(Clock.System.now() + 1.hours)
        scheduler.cancel()

        scheduler.rescheduleIfPending()

        assertEquals(0, registeredAlarms())
    }

    @Test
    fun rescheduleIfPendingIgnoresARestThatAlreadyEnded() {
        scheduler.schedule(Clock.System.now() + 1.hours)
        dropTheSystemAlarmOnly()
        // The stored end moves into the past, as it would be after the alarm fired.
        context.getSharedPreferences(AlarmManagerRestReminderScheduler.PREFS, Context.MODE_PRIVATE).edit()
            .putLong(AlarmManagerRestReminderScheduler.KEY_ENDS_AT, System.currentTimeMillis() - 60_000)
            .commit()

        scheduler.rescheduleIfPending()

        assertEquals(0, registeredAlarms())
    }

    @Test
    fun schedulingAgainReplacesTheAlarmInsteadOfAddingAnother() {
        scheduler.schedule(Clock.System.now() + 1.hours)
        val withOne = registeredAlarms()
        scheduler.cancel()

        scheduler.schedule(Clock.System.now() + 1.hours)
        scheduler.schedule(Clock.System.now() + 2.hours)

        assertTrue(withOne >= 1)
        assertEquals(withOne, registeredAlarms())
    }
}
