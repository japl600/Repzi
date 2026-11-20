package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.ExerciseSource
import com.japl.repzi.core.database.model.ExerciseWithNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseMappersTest {

    private val names = ExerciseNames { "shown name" }

    private fun withNames(source: String, isCustom: Boolean = false) = ExerciseWithNames(
        ExerciseEntity(id = 4, name = "stored", muscleGroup = "LEGS", isCustom = isCustom, source = source),
        names = emptyList(),
    )

    @Test
    fun fromCatalogIsTrueOnlyForWgerRows() {
        assertTrue(withNames(ExerciseSource.WGER).toDomain(names).fromCatalog)
        assertFalse(withNames(ExerciseSource.LOCAL).toDomain(names).fromCatalog)
        assertFalse(withNames(ExerciseSource.LOCAL, isCustom = true).toDomain(names).fromCatalog)
    }

    @Test
    fun mapsTheOtherFieldsAndTheResolvedName() {
        val exercise = withNames(ExerciseSource.WGER).toDomain(names)

        assertEquals(4L, exercise.id)
        assertEquals("shown name", exercise.name)
        assertEquals("LEGS", exercise.muscleGroup.name)
    }
}
