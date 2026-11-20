package com.japl.repzi.core.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InactivitySchedulerTest {

    private val context = NotificationTestSupport.context
    private lateinit var workManager: WorkManager

    @BeforeTest
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        workManager = WorkManager.getInstance(context)
    }

    private fun scheduled(): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(INACTIVITY_WORK_NAME).get()

    @Test
    fun scheduleEnqueuesOneUniquePeriodicWorkOfOneDay() {
        InactivityScheduler(context).schedule()

        val work = scheduled().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(TimeUnit.HOURS.toMillis(24), work.periodicityInfo?.repeatIntervalMillis)
    }

    @Test
    fun schedulingTwiceKeepsASingleWork() {
        val scheduler = InactivityScheduler(context)
        scheduler.schedule()
        val first = scheduled().single().id

        scheduler.schedule()

        assertEquals(first, scheduled().single().id)
    }
}
