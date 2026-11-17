package com.japl.repzi.feature.routines

import com.japl.repzi.core.domain.model.PredefinedRoutines
import kotlin.test.Test
import kotlin.test.assertEquals

class PredefinedRoutineTextsTest {

    @Test
    fun everyPredefinedRoutineHasItsTexts() {
        // A key without texts would crash the list when it is shown.
        assertEquals(PredefinedRoutines.all.map { it.key }.toSet(), PREDEFINED_ROUTINE_TEXTS.keys)
    }
}
