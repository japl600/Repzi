package com.japl.repzi.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class TranslatedName(val language: String, val name: String)

/**
 * An exercise with only the names of its translations. The projection keeps the descriptions (HTML, several KB each)
 * out of the list queries, which run for the whole catalog.
 */
data class ExerciseWithNames(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exercise_id",
        entity = ExerciseTranslationEntity::class,
        projection = ["language", "name"],
    )
    val names: List<TranslatedName>,
)
