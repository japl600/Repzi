package com.japl.repzi.core.notifications

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DelayUntilNextRunTest {

    private val lima = ZoneId.of("America/Lima")
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(zone: ZoneId, year: Int, month: Int, day: Int, hour: Int) =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone)

    @Test
    fun beforeTheHourTheRunIsToday() {
        assertEquals(Duration.ofHours(8), delayUntilNextRun(at(lima, 2026, 11, 20, 10)))
    }

    @Test
    fun afterTheHourTheRunIsTomorrow() {
        assertEquals(Duration.ofHours(22), delayUntilNextRun(at(lima, 2026, 11, 20, 20)))
    }

    @Test
    fun exactlyAtTheHourTheRunIsTomorrow() {
        assertEquals(Duration.ofHours(24), delayUntilNextRun(at(lima, 2026, 11, 20, 18)))
    }

    @Test
    fun theHourOfTheDayIsKeptAcrossTheSpringClockChange() {
        // In Madrid the clock jumps from 02:00 to 03:00 on 29 March 2026, so 20:00 to 18:00 is 21 hours, not 22.
        val now = at(madrid, 2026, 3, 28, 20)

        assertEquals(Duration.ofHours(21), delayUntilNextRun(now))
    }
}
