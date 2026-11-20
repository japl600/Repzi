package com.japl.repzi.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class InactivityRuleTest {

    private val last = Instant.fromEpochMilliseconds(1_700_000_000_000)

    @Test
    fun noNoticeWithoutAFinishedWorkout() {
        assertNull(InactivityRule.daysToReport(last + 30.days, lastWorkoutAt = null, days = 3, lastNoticedFor = null))
    }

    @Test
    fun noNoticeBelowTheThreshold() {
        assertNull(InactivityRule.daysToReport(last + 2.days + 23.hours, last, days = 3, lastNoticedFor = null))
    }

    @Test
    fun noticeAtExactlyTheThreshold() {
        assertEquals(3, InactivityRule.daysToReport(last + 72.hours, last, days = 3, lastNoticedFor = null))
    }

    @Test
    fun reportsTheWholeDaysElapsed() {
        assertEquals(5, InactivityRule.daysToReport(last + 5.days + 2.hours, last, days = 3, lastNoticedFor = null))
    }

    @Test
    fun noNoticeTwiceForTheSameWorkout() {
        assertNull(InactivityRule.daysToReport(last + 10.days, last, days = 3, lastNoticedFor = last))
    }

    @Test
    fun noticeAgainAfterANewerWorkout() {
        val noticedFor = last - 7.days

        assertEquals(4, InactivityRule.daysToReport(last + 4.days, last, days = 3, lastNoticedFor = noticedFor))
    }

    @Test
    fun noNoticeWhenTheLatestWorkoutWasDeleted() {
        // A notice was sent for a workout newer than the one that is now the latest.
        assertNull(InactivityRule.daysToReport(last + 10.days, last, days = 3, lastNoticedFor = last + 2.days))
    }

    @Test
    fun noNoticeWhenTheClockIsBehindTheWorkout() {
        assertNull(InactivityRule.daysToReport(last - 1.days, last, days = 3, lastNoticedFor = null))
    }

    @Test
    fun aOneDayThresholdReportsOneDay() {
        assertEquals(1, InactivityRule.daysToReport(last + 24.hours, last, days = 1, lastNoticedFor = null))
    }
}
