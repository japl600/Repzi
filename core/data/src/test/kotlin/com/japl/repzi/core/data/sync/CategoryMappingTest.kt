package com.japl.repzi.core.data.sync

import com.japl.repzi.core.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryMappingTest {

    @Test
    fun mapsEachWgerCategoryToItsMuscleGroup() {
        val expected = mapOf(
            "Abs" to MuscleGroup.CORE,
            "Arms" to MuscleGroup.ARMS,
            "Back" to MuscleGroup.BACK,
            "Calves" to MuscleGroup.LEGS,
            "Chest" to MuscleGroup.CHEST,
            "Legs" to MuscleGroup.LEGS,
            "Shoulders" to MuscleGroup.SHOULDERS,
        )

        expected.forEach { (category, group) -> assertEquals(group, muscleGroupOf(category), category) }
    }

    @Test
    fun cardioAndUnknownCategoriesAreSkipped() {
        assertNull(muscleGroupOf("Cardio"))
        assertNull(muscleGroupOf("Something new"))
    }
}
