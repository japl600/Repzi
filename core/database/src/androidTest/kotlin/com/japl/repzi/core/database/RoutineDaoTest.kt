package com.japl.repzi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
import com.japl.repzi.core.database.dao.RoutineItem
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.RoutineEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineDaoTest {

    private lateinit var database: RepziDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var routineDao: RoutineDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepziDatabase::class.java).build()
        exerciseDao = database.exerciseDao()
        routineDao = database.routineDao()
    }

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun insertExercises(vararg names: String): List<Long> =
        names.map { exerciseDao.insert(ExerciseEntity(name = it, muscleGroup = "CHEST", isCustom = false)) }

    private fun items(vararg exerciseIds: Long) = exerciseIds.map { RoutineItem(it) }

    private suspend fun exerciseNames(routineId: Long): List<String> =
        routineDao.getById(routineId)!!.items.sortedBy { it.item.position }.map { it.exercise.name }

    @Test
    fun savedRoutineKeepsTheExerciseOrder() = runTest {
        val (a, b, c) = insertExercises("Aperturas", "Banca", "Cruces")

        val id = routineDao.save(RoutineEntity(name = "Push"), items(c, a, b))

        assertEquals(listOf("Cruces", "Aperturas", "Banca"), exerciseNames(id))
    }

    @Test
    fun savingAnExistingRoutineReplacesNameAndExercises() = runTest {
        val (a, b, c) = insertExercises("Aperturas", "Banca", "Cruces")
        val id = routineDao.save(RoutineEntity(name = "Push"), items(a, b))

        routineDao.save(RoutineEntity(id = id, name = "Push day"), items(c))

        assertEquals("Push day", routineDao.getById(id)!!.routine.name)
        assertEquals(listOf("Cruces"), exerciseNames(id))
        assertEquals(1, routineDao.observeAll().first().size)
    }

    private suspend fun restSeconds(routineId: Long): List<Int?> =
        routineDao.getById(routineId)!!.items.sortedBy { it.item.position }.map { it.item.restSeconds }

    @Test
    fun theRestOfEachExerciseIsSavedWithTheRoutine() = runTest {
        val (a, b, c) = insertExercises("Aperturas", "Banca", "Cruces")

        val id = routineDao.save(
            RoutineEntity(name = "Push"),
            listOf(RoutineItem(a, restSeconds = 90), RoutineItem(b), RoutineItem(c, restSeconds = 20)),
        )

        assertEquals(listOf(90, null, 20), restSeconds(id))
    }

    @Test
    fun updatingTheRestChangesOnlyThatExercise() = runTest {
        val (a, b) = insertExercises("Aperturas", "Banca")
        val id = routineDao.save(
            RoutineEntity(name = "Push"),
            listOf(RoutineItem(a, restSeconds = 60), RoutineItem(b, restSeconds = 60)),
        )

        routineDao.updateRest(id, position = 1, restSeconds = 120)
        assertEquals(listOf(60, 120), restSeconds(id))

        routineDao.updateRest(id, position = 0, restSeconds = null)
        assertEquals(listOf(null, 120), restSeconds(id))
    }

    @Test
    fun deletingARoutineRemovesItsJoinRowsButKeepsTheExercises() = runTest {
        val (a, b) = insertExercises("Aperturas", "Banca")
        val id = routineDao.save(RoutineEntity(name = "Push"), items(a, b))

        routineDao.delete(id)

        assertNull(routineDao.getById(id))
        assertEquals(0, database.query("SELECT * FROM routine_exercises", null).use { it.count })
        assertEquals(2, exerciseDao.observeAll().first().size)
    }
}
