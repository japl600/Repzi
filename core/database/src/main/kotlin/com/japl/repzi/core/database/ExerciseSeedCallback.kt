package com.japl.repzi.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Carga el catálogo inicial de ejercicios la primera vez que se crea la base de datos. */
internal class ExerciseSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        SEED.forEach { (name, muscleGroup) ->
            db.execSQL(
                "INSERT INTO exercises (name, muscle_group, is_custom) VALUES (?, ?, 0)",
                arrayOf(name, muscleGroup),
            )
        }
    }

    private companion object {
        // El grupo muscular se guarda como el nombre del enum MuscleGroup del dominio.
        val SEED = listOf(
            "Press de banca" to "CHEST",
            "Press inclinado con mancuernas" to "CHEST",
            "Aperturas" to "CHEST",
            "Dominadas" to "BACK",
            "Remo con barra" to "BACK",
            "Peso muerto" to "BACK",
            "Sentadilla" to "LEGS",
            "Prensa de piernas" to "LEGS",
            "Zancadas" to "LEGS",
            "Press militar" to "SHOULDERS",
            "Elevaciones laterales" to "SHOULDERS",
            "Curl de biceps" to "ARMS",
            "Extension de triceps" to "ARMS",
            "Plancha" to "CORE",
            "Crunch abdominal" to "CORE",
        )
    }
}
