package com.japl.repzi.core.notifications

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
