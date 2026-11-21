package com.japl.repzi.core.ui.component

import com.japl.repzi.core.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightFormatTest {

    @Test
    fun kilogramsAreShownAsTheyAre() {
        assertEquals("62.5", formatWeight(62.5, WeightUnit.KG))
        assertEquals("60", formatWeight(60.0, WeightUnit.KG))
    }

    @Test
    fun aWeightTypedInPoundsIsShownAsTheSameWholeNumber() {
        // 135 lb is stored as 61.2349... kg and must not come back as 134.99999 lb.
        assertEquals("135", formatWeight(WeightUnit.LB.toKg(135.0), WeightUnit.LB))
    }

    @Test
    fun poundsAreRoundedToTwoDecimals() {
        // 22.5 kg is 49.6040... lb.
        assertEquals("49.6", formatWeight(22.5, WeightUnit.LB))
        assertEquals("2.2", formatWeight(1.0, WeightUnit.LB))
    }

    @Test
    fun bodyweightIsZeroInEveryUnit() {
        assertEquals("0", formatWeight(0.0, WeightUnit.LB))
    }
}
