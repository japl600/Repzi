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

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
