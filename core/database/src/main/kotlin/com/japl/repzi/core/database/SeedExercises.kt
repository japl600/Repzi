package com.japl.repzi.core.database

/** One built-in exercise. [key] is stable forever: it is stored in the database and used to look up the translation. */
internal data class SeedExercise(
    val key: String,
    val englishName: String,
    /** The name the first versions stored, used only to recognize those rows in [MIGRATION_4_5]. */
    val legacyName: String,
    /** The name of the domain MuscleGroup enum entry. */
    val muscleGroup: String,
    /** The id of the same exercise in the wger catalog; the sync links the built-in to it (see ExerciseDao.linkBuiltIns). */
    val wgerId: Long,
)

object SeedExercises {

    internal val ALL = listOf(
        SeedExercise("bench_press", "Bench press", "Press de banca", "CHEST", 73),
        SeedExercise("incline_dumbbell_press", "Incline dumbbell press", "Press inclinado con mancuernas", "CHEST", 537),
        SeedExercise("dumbbell_fly", "Dumbbell fly", "Aperturas", "CHEST", 238),
        SeedExercise("pull_up", "Pull-up", "Dominadas", "BACK", 475),
        SeedExercise("barbell_row", "Barbell row", "Remo con barra", "BACK", 1698),
        SeedExercise("deadlift", "Deadlift", "Peso muerto", "BACK", 184),
        SeedExercise("squat", "Squat", "Sentadilla", "LEGS", 615),
        SeedExercise("leg_press", "Leg press", "Prensa de piernas", "LEGS", 371),
        SeedExercise("lunge", "Lunge", "Zancadas", "LEGS", 984),
        SeedExercise("overhead_press", "Overhead press", "Press militar", "SHOULDERS", 566),
        SeedExercise("lateral_raise", "Lateral raise", "Elevaciones laterales", "SHOULDERS", 348),
        SeedExercise("biceps_curl", "Biceps curl", "Curl de biceps", "ARMS", 92),
        SeedExercise("triceps_extension", "Triceps extension", "Extension de triceps", "ARMS", 50),
        SeedExercise("plank", "Plank", "Plancha", "CORE", 458),
        SeedExercise("crunch", "Crunch", "Crunch abdominal", "CORE", 167),
    )

    /** Every key stored in `exercises.seed_key`; the data layer must have a translation for each one. */
    val keys: Set<String> = ALL.map { it.key }.toSet()
}
