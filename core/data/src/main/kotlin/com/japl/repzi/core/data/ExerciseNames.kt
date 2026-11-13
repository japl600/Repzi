package com.japl.repzi.core.data

import android.content.Context
import androidx.annotation.StringRes
import com.japl.repzi.core.database.model.ExerciseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Decides the name shown for an exercise. Keeping this behind an interface lets the mappers stay free
 * of Android and the domain layer receive ready-to-display names.
 */
internal fun interface ExerciseNames {
    fun nameOf(entity: ExerciseEntity): String
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

/**
 * Built-in exercises are translated from string resources in the language the app is using now. Custom
 * exercises, and keys this build does not know, show the stored name.
 */
internal class ResourceExerciseNames @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExerciseNames {

    override fun nameOf(entity: ExerciseEntity): String {
        @StringRes val resource = entity.seedKey?.let(SEED_NAME_RESOURCES::get)
        return if (resource != null) context.getString(resource) else entity.name
    }
}
