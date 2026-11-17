package com.japl.repzi.core.domain.model

/**
 * A routine that ships with the app. It is a template: adding it creates an ordinary routine the user can edit, so
 * nothing else in the app needs to know it came from here. [key] identifies it for its translated name and description;
 * [exerciseKeys] are the seed keys of its exercises, in order.
 */
data class PredefinedRoutine(
    val key: String,
    val exerciseKeys: List<String>,
)

object PredefinedRoutines {

    val all: List<PredefinedRoutine> = listOf(
        PredefinedRoutine(
            key = "full_body",
            exerciseKeys = listOf("squat", "bench_press", "barbell_row", "overhead_press", "plank"),
        ),
        PredefinedRoutine(
            key = "push",
            exerciseKeys = listOf("bench_press", "incline_dumbbell_press", "overhead_press", "lateral_raise", "triceps_extension"),
        ),
        PredefinedRoutine(
            key = "pull",
            exerciseKeys = listOf("pull_up", "barbell_row", "deadlift", "biceps_curl"),
        ),
        PredefinedRoutine(
            key = "legs",
            exerciseKeys = listOf("squat", "leg_press", "lunge"),
        ),
        PredefinedRoutine(
            key = "core",
            exerciseKeys = listOf("plank", "crunch"),
        ),
    )
}
