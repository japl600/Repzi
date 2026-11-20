package com.japl.repzi.core.notifications

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
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InactivityCheckTest {

    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)

    private class RecordingNotifier(var posts: Boolean = true) : InactivityNotifier {
        val notified = mutableListOf<Int>()

        override fun notify(days: Int): Boolean {
            notified += days
            return posts
        }
    }

    private fun session(finishedAt: Instant) = WorkoutSession(
        routineName = "Push",
        startedAt = finishedAt - 1.hours,
        finishedAt = finishedAt,
        exercises = emptyList(),
    )

    private fun check(
        sessions: FakeSessionRepository,
        notifier: RecordingNotifier,
        log: FakeInactivityNoticeLog = FakeInactivityNoticeLog(),
        settings: FakeSettingsRepository = FakeSettingsRepository(initialInactivityReminder = true),
    ) = InactivityCheck(
        sessions = sessions,
        observeReminder = ObserveInactivityReminderUseCase(settings),
        observeDays = ObserveInactivityDaysUseCase(settings),
        noticeLog = log,
        notifier = notifier,
        clock = object : Clock {
            override fun now() = now
        },
    )

    @Test
    fun doesNothingWhenTheReminderIsOff() = runTest {
        val notifier = RecordingNotifier()
        val sessions = FakeSessionRepository(listOf(session(now - 10.days)))

        check(sessions, notifier, settings = FakeSettingsRepository(initialInactivityReminder = false))()

        assertEquals(emptyList(), notifier.notified)
    }

    @Test
    fun doesNothingWithoutFinishedWorkouts() = runTest {
        val notifier = RecordingNotifier()

        check(FakeSessionRepository(), notifier)()

        assertEquals(emptyList(), notifier.notified)
    }

    @Test
    fun notifiesWithTheWholeDaysElapsed() = runTest {
        val notifier = RecordingNotifier()
        val log = FakeInactivityNoticeLog()
        val last = now - 5.days - 2.hours

        check(FakeSessionRepository(listOf(session(last))), notifier, log)()

        assertEquals(listOf(5), notifier.notified)
        assertEquals(listOf(last), log.recorded)
    }

    @Test
    fun usesTheLatestWorkoutOfSeveral() = runTest {
        val notifier = RecordingNotifier()
        val sessions = FakeSessionRepository(listOf(session(now - 20.days), session(now - 4.days), session(now - 9.days)))

        check(sessions, notifier)()

        assertEquals(listOf(4), notifier.notified)
    }

    @Test
    fun notifiesOnceForTheSameLapse() = runTest {
        val notifier = RecordingNotifier()
        val check = check(FakeSessionRepository(listOf(session(now - 5.days))), notifier)

        check()
        check()

        assertEquals(listOf(5), notifier.notified)
    }

    @Test
    fun notifiesAgainAfterANewWorkout() = runTest {
        val notifier = RecordingNotifier()
        val sessions = FakeSessionRepository(listOf(session(now - 20.days)))
        val log = FakeInactivityNoticeLog(initial = now - 20.days)
        val check = check(sessions, notifier, log)
        check()
        assertEquals(emptyList(), notifier.notified)

        sessions.saveSession(session(now - 4.days))
        check()

        assertEquals(listOf(4), notifier.notified)
    }

    @Test
    fun doesNotNotifyAgainWhenTheLatestWorkoutWasDeleted() = runTest {
        val notifier = RecordingNotifier()
        val sessions = FakeSessionRepository(listOf(session(now - 20.days)))
        // A notice was already sent for a newer workout, which the user has since deleted.
        val log = FakeInactivityNoticeLog(initial = now - 10.days)

        check(sessions, notifier, log)()

        assertEquals(emptyList(), notifier.notified)
    }

    @Test
    fun doesNotRecordANoticeThatCouldNotBePosted() = runTest {
        val sessions = FakeSessionRepository(listOf(session(now - 5.days)))
        val log = FakeInactivityNoticeLog()
        val blocked = RecordingNotifier(posts = false)
        check(sessions, blocked, log)()
        assertEquals(emptyList(), log.recorded)

        // The next day the user has allowed notifications again.
        val allowed = RecordingNotifier(posts = true)
        check(sessions, allowed, log)()

        assertEquals(listOf(5), allowed.notified)
    }
}
