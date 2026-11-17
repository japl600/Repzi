package com.japl.repzi.core.data

import com.japl.repzi.core.database.SeedExercises
import com.japl.repzi.core.domain.model.PredefinedRoutines
import kotlin.test.Test
import kotlin.test.assertTrue

class PredefinedRoutinesCatalogTest {

    @Test
    fun everyExerciseOfATemplateIsInTheBuiltInCatalog() {
        // A typo in a key would make the exercise silently disappear from the routine the user adds.
        PredefinedRoutines.all.forEach { template ->
            val unknown = template.exerciseKeys - SeedExercises.keys
            assertTrue(unknown.isEmpty(), "${template.key} uses unknown exercises: $unknown")
        }
    }
}
