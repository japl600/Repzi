package com.japl.repzi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
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

    private suspend fun exerciseNames(routineId: Long): List<String> =
        routineDao.getById(routineId)!!.items.sortedBy { it.item.position }.map { it.exercise.name }

    @Test
    fun savedRoutineKeepsTheExerciseOrder() = runTest {
        val (a, b, c) = insertExercises("Aperturas", "Banca", "Cruces")

        val id = routineDao.save(RoutineEntity(name = "Push"), listOf(c, a, b))

        assertEquals(listOf("Cruces", "Aperturas", "Banca"), exerciseNames(id))
    }

    @Test
    fun savingAnExistingRoutineReplacesNameAndExercises() = runTest {
        val (a, b, c) = insertExercises("Aperturas", "Banca", "Cruces")
        val id = routineDao.save(RoutineEntity(name = "Push"), listOf(a, b))

        routineDao.save(RoutineEntity(id = id, name = "Push day"), listOf(c))

        assertEquals("Push day", routineDao.getById(id)!!.routine.name)
        assertEquals(listOf("Cruces"), exerciseNames(id))
        assertEquals(1, routineDao.observeAll().first().size)
    }

    @Test
    fun deletingARoutineRemovesItsJoinRowsButKeepsTheExercises() = runTest {
        val (a, b) = insertExercises("Aperturas", "Banca")
        val id = routineDao.save(RoutineEntity(name = "Push"), listOf(a, b))

        routineDao.delete(id)

        assertNull(routineDao.getById(id))
        assertEquals(0, database.query("SELECT * FROM routine_exercises", null).use { it.count })
        assertEquals(2, exerciseDao.observeAll().first().size)
    }
}
