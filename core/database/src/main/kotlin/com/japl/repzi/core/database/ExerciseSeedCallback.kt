package com.japl.repzi.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Loads the built-in exercise catalog the first time the database is created. */
internal class ExerciseSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        SeedExercises.ALL.forEach { exercise ->
            db.execSQL(
                "INSERT INTO exercises (name, muscle_group, is_custom, seed_key) VALUES (?, ?, 0, ?)",
                arrayOf(exercise.englishName, exercise.muscleGroup, exercise.key),
            )
        }
    }
}
