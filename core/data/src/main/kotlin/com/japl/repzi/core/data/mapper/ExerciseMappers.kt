package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.ExerciseSource
import com.japl.repzi.core.database.model.ExerciseTranslationEntity
import com.japl.repzi.core.database.model.ExerciseWithNames
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.model.MuscleGroup

internal fun ExerciseWithNames.toDomain(names: ExerciseNames) = Exercise(
    id = exercise.id,
    name = names.nameOf(this),
    muscleGroup = MuscleGroup.valueOf(exercise.muscleGroup),
    isCustom = exercise.isCustom,
    seedKey = exercise.seedKey,
    fromCatalog = exercise.source == ExerciseSource.WGER,
)

/**
 * The detail of an exercise. The description, author and license come from the translation in [language], else English;
 * an exercise with no translation (one the user created) has none of them. Blank text counts as missing.
 */
internal fun ExerciseEntity.toDetail(
    name: String,
    translations: List<ExerciseTranslationEntity>,
    language: String,
): ExerciseDetail {
    val shown = translations.firstOrNull { it.language == language } ?: translations.firstOrNull { it.language == "en" }
    return ExerciseDetail(
        id = id,
        name = name,
        muscleGroup = MuscleGroup.valueOf(muscleGroup),
        isCustom = isCustom,
        description = shown?.description?.takeIf { it.isNotBlank() },
        author = shown?.author?.takeIf { it.isNotBlank() },
        licenseName = shown?.licenseName,
        licenseUrl = shown?.licenseUrl,
        sourceUrl = remoteId?.let { "$WGER_EXERCISE_URL$it/view/" },
    )
}

private const val WGER_EXERCISE_URL = "https://wger.de/en/exercise/"
