package com.japl.repzi.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {

    @Test
    fun wholeNumbersLoseTheDecimalPart() {
        assertEquals("60", formatNumber(60.0))
        assertEquals("1250", formatNumber(1_250.0))
        assertEquals("0", formatNumber(0.0))
    }

    @Test
    fun fractionsAreKept() {
        assertEquals("62.5", formatNumber(62.5))
    }
}
