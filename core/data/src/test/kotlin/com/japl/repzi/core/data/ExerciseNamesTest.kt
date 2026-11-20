package com.japl.repzi.core.data

import com.japl.repzi.core.database.SeedExercises
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.ExerciseWithNames
import com.japl.repzi.core.database.model.TranslatedName
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseNamesTest {

    @Test
    fun everySeedKeyHasATranslation() {
        // A key without a string would silently fall back to the English name stored in the database.
        assertEquals(SeedExercises.keys, SEED_NAME_RESOURCES.keys)
    }

    private val names = listOf(TranslatedName("en", "Bench Press"), TranslatedName("es", "Press de Banca"))

    @Test
    fun pickNameUsesTheAppLanguageFirst() {
        assertEquals("Press de Banca", pickName(names, language = "es", fallback = "stored"))
    }

    @Test
    fun pickNameFallsBackToEnglish() {
        assertEquals("Bench Press", pickName(names, language = "fr", fallback = "stored"))
        assertEquals("Only English", pickName(listOf(TranslatedName("en", "Only English")), "es", "stored"))
    }

    @Test
    fun pickNameFallsBackToTheStoredNameWhenThereAreNoTranslations() {
        assertEquals("stored", pickName(emptyList(), language = "es", fallback = "stored"))
    }

    private fun exercise(seedKey: String?, name: String, names: List<TranslatedName>) =
        ExerciseWithNames(
            ExerciseEntity(id = 1, name = name, muscleGroup = "CHEST", isCustom = seedKey == null, seedKey = seedKey),
            names,
        )

    private fun resolver(seedName: (String) -> String?, language: String = "es") =
        TranslatedExerciseNames(seedName = seedName, language = { language })

    @Test
    fun builtInKeepsItsResourceNameEvenWhenTranslationsExist() {
        val resolver = resolver(seedName = { "Press de banca (recurso)" })

        assertEquals("Press de banca (recurso)", resolver.nameOf(exercise("bench_press", "Bench press", names)))
    }

    @Test
    fun aKeyThisBuildDoesNotKnowUsesTheTranslation() {
        val resolver = resolver(seedName = { null })

        assertEquals("Press de Banca", resolver.nameOf(exercise("future_key", "Bench press", names)))
    }

    @Test
    fun customExerciseShowsItsTypedName() {
        val resolver = resolver(seedName = { error("a custom exercise has no seed key") })

        assertEquals("Mi ejercicio", resolver.nameOf(exercise(null, "Mi ejercicio", emptyList())))
    }

    @Test
    fun catalogExerciseFollowsTheAppLanguage() {
        val catalog = exercise(null, "Bench Press", names)

        assertEquals("Press de Banca", resolver({ null }, "es").nameOf(catalog))
        assertEquals("Bench Press", resolver({ null }, "en").nameOf(catalog))
    }
}
