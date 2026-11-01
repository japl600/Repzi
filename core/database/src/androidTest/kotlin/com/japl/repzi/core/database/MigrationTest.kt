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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
