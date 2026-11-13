package com.japl.repzi.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class RestTimeFormatTest {

    @Test
    fun formatsSecondsUnderAMinuteWithALeadingZeroMinute() {
        assertEquals("0:10", formatRestTime(10))
        assertEquals("0:05", formatRestTime(5))
        assertEquals("0:00", formatRestTime(0))
    }

    @Test
    fun formatsWholeAndPartialMinutes() {
        assertEquals("1:00", formatRestTime(60))
        assertEquals("1:30", formatRestTime(90))
        assertEquals("3:00", formatRestTime(180))
    }

    @Test
    fun aNegativeValueIsShownAsZero() {
        assertEquals("0:00", formatRestTime(-4))
    }
}
