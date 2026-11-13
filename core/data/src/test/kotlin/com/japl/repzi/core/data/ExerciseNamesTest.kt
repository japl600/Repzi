package com.japl.repzi.core.data

import com.japl.repzi.core.database.SeedExercises
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseNamesTest {

    @Test
    fun everySeedKeyHasATranslation() {
        // A key without a string would silently fall back to the English name stored in the database.
        assertEquals(SeedExercises.keys, SEED_NAME_RESOURCES.keys)
    }
}
