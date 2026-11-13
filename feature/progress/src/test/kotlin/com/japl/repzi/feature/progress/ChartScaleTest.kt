package com.japl.repzi.feature.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartScaleTest {

    @Test
    fun roundsTheAxisToTheNearestStep() {
        val scale = chartScale(62.5, 78.0)

        // Range 15.5 over four intervals needs a step of 5.
        assertEquals(ChartScale(min = 60.0, max = 80.0, step = 5.0), scale)
        assertEquals(listOf(60.0, 65.0, 70.0, 75.0, 80.0), scale.ticks)
    }

    @Test
    fun anExactMultipleStaysOnTheEdge() {
        val scale = chartScale(60.0, 80.0)

        assertEquals(60.0, scale.min)
        assertEquals(80.0, scale.max)
    }

    @Test
    fun aFlatSeriesStillGetsARange() {
        val scale = chartScale(100.0, 100.0)

        assertTrue(scale.min <= 100.0 && scale.max >= 100.0)
        assertTrue(scale.ticks.size >= 2)
    }

    @Test
    fun theAxisDoesNotHaveToStartAtZero() {
        val scale = chartScale(100.0, 105.0)

        // A narrow range zooms in so small gains are still visible.
        assertEquals(ChartScale(min = 100.0, max = 106.0, step = 2.0), scale)
    }

    @Test
    fun repsNeverGetAFractionalStep() {
        // A range of 2 would give a step of 0.5, which would show half reps on the axis.
        assertEquals(ChartScale(min = 8.0, max = 10.0, step = 1.0), chartScale(8.0, 10.0, wholeNumbers = true))
        // And 2.5-sized steps are skipped too: 0..10 uses 5 instead.
        assertEquals(2.5, chartScale(0.0, 10.0).step)
        assertEquals(5.0, chartScale(0.0, 10.0, wholeNumbers = true).step)
    }

    @Test
    fun smallRangesGetAWholeKilogramStep() {
        val scale = chartScale(20.0, 22.5)

        assertEquals(ChartScale(min = 20.0, max = 23.0, step = 1.0), scale)
    }
}
