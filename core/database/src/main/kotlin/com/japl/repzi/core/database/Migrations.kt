package com.japl.repzi.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds routines and the routine_exercises join table. Existing exercises are left untouched. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routines` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine_exercises` " +
                "(`routine_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL, " +
                "PRIMARY KEY(`routine_id`, `position`), " +
                "FOREIGN KEY(`routine_id`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_routine_exercises_exercise_id` ON `routine_exercises` (`exercise_id`)",
        )
    }
}

/** Adds workout_sessions and logged_sets. Existing data is left untouched. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_sessions` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routine_name` TEXT NOT NULL, " +
                "`started_at` INTEGER NOT NULL, `finished_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `logged_sets` " +
                "(`session_id` INTEGER NOT NULL, `exercise_position` INTEGER NOT NULL, `set_index` INTEGER NOT NULL, " +
                "`exercise_id` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weight_kg` REAL NOT NULL, " +
                "PRIMARY KEY(`session_id`, `exercise_position`, `set_index`), " +
                "FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_logged_sets_exercise_id` ON `logged_sets` (`exercise_id`)",
        )
    }
}

/** Adds workout_drafts and draft_sets, so a workout in progress survives the app being closed. */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_drafts` " +
                "(`routine_id` INTEGER NOT NULL, `started_at` INTEGER NOT NULL, PRIMARY KEY(`routine_id`), " +
                "FOREIGN KEY(`routine_id`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `draft_sets` " +
                "(`routine_id` INTEGER NOT NULL, `exercise_position` INTEGER NOT NULL, `set_index` INTEGER NOT NULL, " +
                "`exercise_id` INTEGER NOT NULL, `weight` TEXT NOT NULL, `reps` TEXT NOT NULL, " +
                "PRIMARY KEY(`routine_id`, `exercise_position`, `set_index`), " +
                "FOREIGN KEY(`routine_id`) REFERENCES `workout_drafts`(`routine_id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_draft_sets_exercise_id` ON `draft_sets` (`exercise_id`)",
        )
    }
}

/**
 * Adds exercises.seed_key and tags the built-in exercises with it, so their names can be translated.
 * The built-in rows were stored in Spanish, so they are recognized by that name (only the ones that are
 * not custom) and renamed to the English fallback. Exercises the user created keep a null key.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `seed_key` TEXT")
        SeedExercises.ALL.forEach { exercise ->
            db.execSQL(
                "UPDATE exercises SET seed_key = ?, name = ? WHERE name = ? AND is_custom = 0",
                arrayOf(exercise.key, exercise.englishName, exercise.legacyName),
            )
        }
    }
}

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
