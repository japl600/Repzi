package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.ExerciseSource
import com.japl.repzi.core.database.model.ExerciseTranslationEntity
import com.japl.repzi.core.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseDetailMapperTest {

    private val catalogExercise = ExerciseEntity(
        id = 3,
        name = "Kettlebell swing",
        muscleGroup = "CORE",
        isCustom = false,
        remoteId = 9,
        source = ExerciseSource.WGER,
    )

    private fun translation(language: String, description: String = "<p>$language</p>", author: String? = "author-$language") =
        ExerciseTranslationEntity(
            exerciseId = 3,
            language = language,
            name = "name-$language",
            description = description,
            author = author,
            licenseName = "CC-BY-SA 4 ($language)",
            licenseUrl = "https://license/$language",
        )

    @Test
    fun usesTheTranslationInTheAppLanguage() {
        val detail = catalogExercise.toDetail("Swing", listOf(translation("en"), translation("es")), language = "es")

        assertEquals("<p>es</p>", detail.description)
        assertEquals("author-es", detail.author)
        assertEquals("CC-BY-SA 4 (es)", detail.licenseName)
        assertEquals("https://license/es", detail.licenseUrl)
    }

    @Test
    fun fallsBackToEnglishWhenTheAppLanguageHasNoTranslation() {
        val detail = catalogExercise.toDetail("Swing", listOf(translation("en")), language = "es")

        assertEquals("<p>en</p>", detail.description)
        assertEquals("author-en", detail.author)
    }

    @Test
    fun usesAnyTranslationWhenNeitherTheAppLanguageNorEnglishExists() {
        // A Spanish-only exercise in an English app: its attribution must not be lost.
        val detail = catalogExercise.toDetail("Swing", listOf(translation("es")), language = "en")

        assertEquals("<p>es</p>", detail.description)
        assertEquals("author-es", detail.author)
        assertEquals("CC-BY-SA 4 (es)", detail.licenseName)
        assertEquals("https://license/es", detail.licenseUrl)
    }

    @Test
    fun mapsTheExerciseFieldsAndTheSourceLink() {
        val detail = catalogExercise.toDetail("Swing", listOf(translation("en")), language = "en")

        assertEquals(3L, detail.id)
        assertEquals("Swing", detail.name)
        assertEquals(MuscleGroup.CORE, detail.muscleGroup)
        assertEquals(false, detail.isCustom)
        assertEquals("https://wger.de/en/exercise/9/view/", detail.sourceUrl)
    }

    @Test
    fun customExerciseHasNoAuthorLicenseOrSource() {
        val custom = ExerciseEntity(id = 8, name = "Mi remo", muscleGroup = "BACK", isCustom = true)

        val detail = custom.toDetail("Mi remo", emptyList(), language = "es")

        assertEquals(true, detail.isCustom)
        assertNull(detail.description)
        assertNull(detail.author)
        assertNull(detail.licenseName)
        assertNull(detail.licenseUrl)
        assertNull(detail.sourceUrl)
    }

    @Test
    fun blankDescriptionAndAuthorBecomeNull() {
        val detail = catalogExercise.toDetail("Swing", listOf(translation("en", description = "  ", author = "")), language = "en")

        assertNull(detail.description)
        assertNull(detail.author)
    }
}
