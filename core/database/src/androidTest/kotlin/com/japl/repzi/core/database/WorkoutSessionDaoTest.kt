package com.japl.repzi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.WorkoutSessionDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.LoggedSetEntity
import com.japl.repzi.core.database.model.WorkoutSessionEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionDaoTest {

    private lateinit var database: RepziDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var sessionDao: WorkoutSessionDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepziDatabase::class.java).build()
        exerciseDao = database.exerciseDao()
        sessionDao = database.workoutSessionDao()
    }

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun insertExercise(name: String): Long =
        exerciseDao.insert(ExerciseEntity(name = name, muscleGroup = "CHEST", isCustom = false))

    // The session id is unknown before the insert; the DAO overwrites the 0 placeholder.
    private fun set(exercisePosition: Int, setIndex: Int, exerciseId: Long, reps: Int, weightKg: Double) =
        LoggedSetEntity(0, exercisePosition, setIndex, exerciseId, reps, weightKg)

    @Test
    fun savedSessionKeepsItsSetsLinkedToTheGeneratedId() = runTest {
        val bench = insertExercise("Banca")
        val squat = insertExercise("Sentadilla")

        val id = sessionDao.save(
            WorkoutSessionEntity(routineName = "Full body", startedAt = 1_000, finishedAt = 5_000),
            listOf(
                set(0, 0, bench, reps = 8, weightKg = 60.0),
                set(0, 1, bench, reps = 6, weightKg = 62.5),
                set(1, 0, squat, reps = 5, weightKg = 100.0),
            ),
        )

        val saved = sessionDao.getById(id)!!
        assertEquals("Full body", saved.session.routineName)
        assertEquals(3, saved.sets.size)
        assertEquals(setOf(id), saved.sets.map { it.set.sessionId }.toSet())
        assertEquals(
            listOf(Triple("Banca", 8, 60.0), Triple("Banca", 6, 62.5), Triple("Sentadilla", 5, 100.0)),
            saved.sets
                .sortedWith(compareBy({ it.set.exercisePosition }, { it.set.setIndex }))
                .map { Triple(it.exercise.name, it.set.reps, it.set.weightKg) },
        )
    }

    @Test
    fun theSameExerciseCanAppearTwiceInOneSession() = runTest {
        val bench = insertExercise("Banca")

        val id = sessionDao.save(
            WorkoutSessionEntity(routineName = "Push", startedAt = 0, finishedAt = 1),
            listOf(set(0, 0, bench, 8, 60.0), set(1, 0, bench, 10, 40.0)),
        )

        assertEquals(2, sessionDao.getById(id)!!.sets.size)
    }

    @Test
    fun unknownSessionIsNull() = runTest {
        assertNull(sessionDao.getById(42))
    }
}
