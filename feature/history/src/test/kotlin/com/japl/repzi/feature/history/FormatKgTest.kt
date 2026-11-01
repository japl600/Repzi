package com.japl.repzi.feature.history

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatKgTest {

    @Test
    fun wholeWeightsDropTheDecimalPart() {
        assertEquals("60", formatKg(60.0))
        assertEquals("1250", formatKg(1_250.0))
        assertEquals("0", formatKg(0.0))
    }

    @Test
    fun fractionalWeightsKeepTheirDecimals() {
        assertEquals("62.5", formatKg(62.5))
    }
}
