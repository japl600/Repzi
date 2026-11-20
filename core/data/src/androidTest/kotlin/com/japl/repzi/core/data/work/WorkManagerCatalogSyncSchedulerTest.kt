package com.japl.repzi.core.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerCatalogSyncSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var workManager: WorkManager

    @BeforeTest
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        workManager = WorkManager.getInstance(context)
    }

    private fun scheduled(): List<WorkInfo> = workManager.getWorkInfosForUniqueWork("catalog-sync").get()

    @Test
    fun scheduleEnqueuesOneUniquePeriodicWorkOfSevenDaysThatNeedsAnUnmeteredNetwork() {
        WorkManagerCatalogSyncScheduler(context).schedule()

        val work = scheduled().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.UNMETERED, work.constraints.requiredNetworkType)
        assertEquals(TimeUnit.DAYS.toMillis(7), work.periodicityInfo?.repeatIntervalMillis)
    }

    @Test
    fun schedulingTwiceKeepsASingleWork() {
        val scheduler = WorkManagerCatalogSyncScheduler(context)
        scheduler.schedule()
        val first = scheduled().single().id

        scheduler.schedule()

        assertEquals(first, scheduled().single().id)
    }
}
