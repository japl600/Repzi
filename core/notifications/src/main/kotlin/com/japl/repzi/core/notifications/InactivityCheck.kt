package com.japl.repzi.core.notifications

import com.japl.repzi.core.domain.InactivityRule
import com.japl.repzi.core.domain.repository.InactivityNoticeLog
import com.japl.repzi.core.domain.repository.SessionRepository
import com.japl.repzi.core.domain.usecase.ObserveInactivityDaysUseCase
import com.japl.repzi.core.domain.usecase.ObserveInactivityReminderUseCase
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

/**
 * The daily check. It always runs; the setting is read now, so a reminder that is off costs nothing but this read.
 * The notice is recorded only once it was really posted, so a blocked day is retried the next day.
 */
internal class InactivityCheck @Inject constructor(
    private val sessions: SessionRepository,
    private val observeReminder: ObserveInactivityReminderUseCase,
    private val observeDays: ObserveInactivityDaysUseCase,
    private val noticeLog: InactivityNoticeLog,
    private val notifier: InactivityNotifier,
    private val clock: Clock,
) {

    suspend operator fun invoke() {
        if (!observeReminder().first()) return
        val lastWorkoutAt = sessions.observeSessions().first().maxOfOrNull { it.finishedAt } ?: return
        val days = InactivityRule.daysToReport(
            now = clock.now(),
            lastWorkoutAt = lastWorkoutAt,
            days = observeDays().first(),
            lastNoticedFor = noticeLog.lastNoticeFor(),
        ) ?: return
        if (notifier.notify(days)) noticeLog.record(lastWorkoutAt)
    }
}
