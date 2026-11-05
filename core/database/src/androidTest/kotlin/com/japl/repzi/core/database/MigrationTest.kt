package com.japl.repzi.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RepziDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2KeepsExercisesAndCreatesRoutineTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO exercises (name, muscle_group, is_custom) VALUES ('Sentadilla', 'LEGS', 0)")
            close()
        }

        // runMigrationsAndValidate checks the migrated schema against the exported 2.json.
        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT name FROM exercises").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Sentadilla", it.getString(0))
        }
        db.execSQL("INSERT INTO routines (name) VALUES ('Legs')")
        db.query("SELECT COUNT(*) FROM routine_exercises").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate2To3KeepsRoutinesAndCreatesSessionTables() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL("INSERT INTO exercises (name, muscle_group, is_custom) VALUES ('Sentadilla', 'LEGS', 0)")
            execSQL("INSERT INTO routines (name) VALUES ('Legs')")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        db.query("SELECT name FROM routines").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Legs", it.getString(0))
        }
        db.execSQL("INSERT INTO workout_sessions (routine_name, started_at, finished_at) VALUES ('Legs', 1, 2)")
        db.execSQL("INSERT INTO logged_sets VALUES (1, 0, 0, 1, 5, 100.0)")
    }

    @Test
    fun migrate3To4KeepsSessionsAndCreatesDraftTables() {
        helper.createDatabase(DB_NAME, 3).apply {
            execSQL("INSERT INTO exercises (name, muscle_group, is_custom) VALUES ('Sentadilla', 'LEGS', 0)")
            execSQL("INSERT INTO routines (name) VALUES ('Legs')")
            execSQL("INSERT INTO workout_sessions (routine_name, started_at, finished_at) VALUES ('Legs', 1, 2)")
            execSQL("INSERT INTO logged_sets VALUES (1, 0, 0, 1, 5, 100.0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        db.query("SELECT COUNT(*) FROM logged_sets").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.execSQL("INSERT INTO workout_drafts VALUES (1, 1000)")
        db.execSQL("INSERT INTO draft_sets VALUES (1, 0, 0, 1, '100', '5')")
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
