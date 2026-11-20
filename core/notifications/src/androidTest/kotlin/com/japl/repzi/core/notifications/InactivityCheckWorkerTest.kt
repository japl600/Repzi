package com.japl.repzi.core.notifications

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.ObserveInactivityDaysUseCase
import com.japl.repzi.core.domain.usecase.ObserveInactivityReminderUseCase
import com.japl.repzi.core.testing.FakeInactivityNoticeLog
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InactivityCheckWorkerTest {

    private val context = NotificationTestSupport.context

    @Test
    fun runsTheCheckAndSucceeds() {
        val now = Clock.System.now()
        val notified = mutableListOf<Int>()
        val settings = FakeSettingsRepository(initialInactivityReminder = true)
        val check = InactivityCheck(
            sessions = FakeSessionRepository(
                listOf(WorkoutSession(routineName = "Push", startedAt = now - 6.days - 1.hours, finishedAt = now - 6.days, exercises = emptyList())),
            ),
            observeReminder = ObserveInactivityReminderUseCase(settings),
            observeDays = ObserveInactivityDaysUseCase(settings),
            noticeLog = FakeInactivityNoticeLog(),
            notifier = { days -> notified += days; true },
            clock = Clock.System,
        )
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = InactivityCheckWorker(appContext, workerParameters, check)
        }
        val worker = TestListenableWorkerBuilder<InactivityCheckWorker>(context).setWorkerFactory(factory).build()

        val result = runBlocking { worker.doWork() }

        assertEquals(Result.success(), result)
        assertEquals(listOf(6), notified)
    }
}
