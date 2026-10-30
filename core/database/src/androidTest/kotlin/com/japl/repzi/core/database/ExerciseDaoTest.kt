package com.japl.repzi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.ExerciseEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: RepziDatabase
    private lateinit var dao: ExerciseDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepziDatabase::class.java).build()
        dao = database.exerciseDao()
    }

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun insertedExercisesAreObservedOrderedByName() = runTest {
        dao.insert(ExerciseEntity(name = "Sentadilla", muscleGroup = "LEGS", isCustom = false))
        dao.insert(ExerciseEntity(name = "Aperturas", muscleGroup = "CHEST", isCustom = false))

        assertEquals(listOf("Aperturas", "Sentadilla"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun filtersByMuscleGroup() = runTest {
        dao.insert(ExerciseEntity(name = "Sentadilla", muscleGroup = "LEGS", isCustom = false))
        dao.insert(ExerciseEntity(name = "Aperturas", muscleGroup = "CHEST", isCustom = false))

        assertEquals(listOf("Sentadilla"), dao.observeByMuscleGroup("LEGS").first().map { it.name })
    }
}
