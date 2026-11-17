package com.japl.repzi.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PredefinedRoutinesTest {

    @Test
    fun keysAreUnique() {
        val keys = PredefinedRoutines.all.map { it.key }
        assertEquals(keys.toSet().size, keys.size)
    }

    @Test
    fun everyTemplateHasExercisesAndNoRepeats() {
        PredefinedRoutines.all.forEach { template ->
            assertTrue(template.exerciseKeys.isNotEmpty(), "${template.key} is empty")
            assertEquals(template.exerciseKeys.toSet().size, template.exerciseKeys.size, "${template.key} repeats an exercise")
        }
    }
}
