package com.japl.repzi.core.data

import android.content.Context
import com.japl.repzi.core.database.model.ExerciseWithNames
import com.japl.repzi.core.database.model.TranslatedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Decides the name shown for an exercise. Keeping this behind an interface lets the mappers stay free
 * of Android and the domain layer receive ready-to-display names.
 */
internal fun interface ExerciseNames {
    fun nameOf(exercise: ExerciseWithNames): String
}

/** One string resource per key in [com.japl.repzi.core.database.SeedExercises]. */
internal val SEED_NAME_RESOURCES: Map<String, Int> = mapOf(
    "bench_press" to R.string.exercise_bench_press,
    "incline_dumbbell_press" to R.string.exercise_incline_dumbbell_press,
    "dumbbell_fly" to R.string.exercise_dumbbell_fly,
    "pull_up" to R.string.exercise_pull_up,
    "barbell_row" to R.string.exercise_barbell_row,
    "deadlift" to R.string.exercise_deadlift,
    "squat" to R.string.exercise_squat,
    "leg_press" to R.string.exercise_leg_press,
    "lunge" to R.string.exercise_lunge,
    "overhead_press" to R.string.exercise_overhead_press,
    "lateral_raise" to R.string.exercise_lateral_raise,
    "biceps_curl" to R.string.exercise_biceps_curl,
    "triceps_extension" to R.string.exercise_triceps_extension,
    "plank" to R.string.exercise_plank,
    "crunch" to R.string.exercise_crunch,
)

/** The translation for [language], else the English one, else [fallback] (the name stored with the exercise). */
internal fun pickName(names: List<TranslatedName>, language: String, fallback: String): String =
    names.firstOrNull { it.language == language }?.name
        ?: names.firstOrNull { it.language == "en" }?.name
        ?: fallback

/**
 * In order: a built-in's string resource ([seedName] looks it up by seed key); the catalog translation for the
 * app language, then English; the stored name (what the user typed for a custom exercise, or the English fallback).
 */
internal class TranslatedExerciseNames(
    private val seedName: (String) -> String?,
    private val language: () -> String,
) : ExerciseNames {

    override fun nameOf(exercise: ExerciseWithNames): String {
        val entity = exercise.exercise
        entity.seedKey?.let(seedName)?.let { return it }
        return pickName(exercise.names, language(), entity.name)
    }
}

/** Android flavour: seed names from string resources, language from [AppLanguage] (the one place that reads it). */
internal class ResourceExerciseNames @Inject constructor(
    @param:ApplicationContext private val context: Context,
    appLanguage: AppLanguage,
) : ExerciseNames by TranslatedExerciseNames(
    seedName = { key -> SEED_NAME_RESOURCES[key]?.let(context::getString) },
    language = { appLanguage() },
)
