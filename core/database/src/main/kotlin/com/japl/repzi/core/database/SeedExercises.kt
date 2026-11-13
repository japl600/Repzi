package com.japl.repzi.core.database

/** One built-in exercise. [key] is stable forever: it is stored in the database and used to look up the translation. */
internal data class SeedExercise(
    val key: String,
    val englishName: String,
    /** The name the first versions stored, used only to recognize those rows in [MIGRATION_4_5]. */
    val legacyName: String,
    /** The name of the domain MuscleGroup enum entry. */
    val muscleGroup: String,
)

object SeedExercises {

    internal val ALL = listOf(
        SeedExercise("bench_press", "Bench press", "Press de banca", "CHEST"),
        SeedExercise("incline_dumbbell_press", "Incline dumbbell press", "Press inclinado con mancuernas", "CHEST"),
        SeedExercise("dumbbell_fly", "Dumbbell fly", "Aperturas", "CHEST"),
        SeedExercise("pull_up", "Pull-up", "Dominadas", "BACK"),
        SeedExercise("barbell_row", "Barbell row", "Remo con barra", "BACK"),
        SeedExercise("deadlift", "Deadlift", "Peso muerto", "BACK"),
        SeedExercise("squat", "Squat", "Sentadilla", "LEGS"),
        SeedExercise("leg_press", "Leg press", "Prensa de piernas", "LEGS"),
        SeedExercise("lunge", "Lunge", "Zancadas", "LEGS"),
        SeedExercise("overhead_press", "Overhead press", "Press militar", "SHOULDERS"),
        SeedExercise("lateral_raise", "Lateral raise", "Elevaciones laterales", "SHOULDERS"),
        SeedExercise("biceps_curl", "Biceps curl", "Curl de biceps", "ARMS"),
        SeedExercise("triceps_extension", "Triceps extension", "Extension de triceps", "ARMS"),
        SeedExercise("plank", "Plank", "Plancha", "CORE"),
        SeedExercise("crunch", "Crunch", "Crunch abdominal", "CORE"),
    )

    /** Every key stored in `exercises.seed_key`; the data layer must have a translation for each one. */
    val keys: Set<String> = ALL.map { it.key }.toSet()
}
