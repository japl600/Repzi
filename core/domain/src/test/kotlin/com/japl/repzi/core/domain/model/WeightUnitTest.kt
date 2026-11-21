package com.japl.repzi.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WeightUnitTest {

    @Test
    fun kilogramsAreTheirOwnUnit() {
        assertEquals(80.0, WeightUnit.KG.fromKg(80.0))
        assertEquals(80.0, WeightUnit.KG.toKg(80.0))
    }

    @Test
    fun aPoundIsAboutHalfAKilogram() {
        assertEquals(0.45359237, WeightUnit.LB.toKg(1.0), absoluteTolerance = 1e-12)
        assertEquals(2.2046226218, WeightUnit.LB.fromKg(1.0), absoluteTolerance = 1e-9)
    }

    @Test
    fun aWeightTypedInPoundsComesBackAsTheSameNumberOfPounds() {
        val stored = WeightUnit.LB.toKg(135.0)

        assertEquals(135.0, WeightUnit.LB.fromKg(stored), absoluteTolerance = 1e-9)
    }

    @Test
    fun zeroIsBodyweightInEveryUnit() {
        assertEquals(0.0, WeightUnit.LB.toKg(0.0))
        assertEquals(0.0, WeightUnit.LB.fromKg(0.0))
    }
}
