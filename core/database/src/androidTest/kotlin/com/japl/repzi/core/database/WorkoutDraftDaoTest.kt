package com.japl.repzi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
import com.japl.repzi.core.database.dao.WorkoutDraftDao
import com.japl.repzi.core.database.model.DraftSetEntity
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.RoutineEntity
import com.japl.repzi.core.database.model.WorkoutDraftEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDraftDaoTest {

    private lateinit var database: RepziDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var routineDao: RoutineDao
    private lateinit var draftDao: WorkoutDraftDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepziDatabase::class.java).build()
        exerciseDao = database.exerciseDao()
        routineDao = database.routineDao()
        draftDao = database.workoutDraftDao()
    }

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun insertExercise(name: String): Long =
        exerciseDao.insert(ExerciseEntity(name = name, muscleGroup = "CHEST", isCustom = false))

    private suspend fun insertRoutine(name: String, exerciseIds: List<Long>): Long =
        routineDao.save(RoutineEntity(name = name), exerciseIds)

    private fun set(routineId: Long, exercisePosition: Int, setIndex: Int, exerciseId: Long, weight: String, reps: String) =
        DraftSetEntity(routineId, exercisePosition, setIndex, exerciseId, weight, reps)

    private fun draftSetCount() = database.query("SELECT * FROM draft_sets", null).use { it.count }

    @Test
    fun savedDraftKeepsItsSetsAsTypedIncludingEmptyOnes() = runTest {
        val bench = insertExercise("Banca")
        val squat = insertExercise("Sentadilla")
        val routine = insertRoutine("Full body", listOf(bench, squat))

        draftDao.save(
            WorkoutDraftEntity(routine, startedAt = 1_000),
            listOf(
                set(routine, 0, 0, bench, weight = "60", reps = "8"),
                set(routine, 0, 1, bench, weight = "", reps = ""),
                set(routine, 1, 0, squat, weight = "100.", reps = "5"),
            ),
        )

        val saved = draftDao.getByRoutine(routine)!!
        assertEquals(1_000, saved.draft.startedAt)
        assertEquals(
            listOf(Triple(bench, "60", "8"), Triple(bench, "", ""), Triple(squat, "100.", "5")),
            saved.sets
                .sortedWith(compareBy({ it.exercisePosition }, { it.setIndex }))
                .map { Triple(it.exerciseId, it.weight, it.reps) },
        )
    }

    @Test
    fun savingAgainReplacesTheWholeDraftOfThatRoutine() = runTest {
        val bench = insertExercise("Banca")
        val routine = insertRoutine("Push", listOf(bench))
        draftDao.save(
            WorkoutDraftEntity(routine, startedAt = 1_000),
            listOf(set(routine, 0, 0, bench, "60", "8"), set(routine, 0, 1, bench, "60", "8")),
        )

        draftDao.save(WorkoutDraftEntity(routine, startedAt = 1_000), listOf(set(routine, 0, 0, bench, "62.5", "6")))

        val saved = draftDao.getByRoutine(routine)!!
        assertEquals(listOf("62.5"), saved.sets.map { it.weight })
        assertEquals(1, draftSetCount())
    }

    @Test
    fun eachRoutineKeepsItsOwnDraft() = runTest {
        val bench = insertExercise("Banca")
        val push = insertRoutine("Push", listOf(bench))
        val chest = insertRoutine("Chest", listOf(bench))
        draftDao.save(WorkoutDraftEntity(push, startedAt = 1), listOf(set(push, 0, 0, bench, "60", "8")))
        draftDao.save(WorkoutDraftEntity(chest, startedAt = 2), listOf(set(chest, 0, 0, bench, "40", "12")))

        assertEquals("60", draftDao.getByRoutine(push)!!.sets.single().weight)
        assertEquals("40", draftDao.getByRoutine(chest)!!.sets.single().weight)
    }

    @Test
    fun unknownRoutineHasNoDraft() = runTest {
        assertNull(draftDao.getByRoutine(42))
    }

    @Test
    fun deletingADraftRemovesItsSetsButKeepsRoutineAndExercises() = runTest {
        val bench = insertExercise("Banca")
        val routine = insertRoutine("Push", listOf(bench))
        draftDao.save(WorkoutDraftEntity(routine, startedAt = 1), listOf(set(routine, 0, 0, bench, "60", "8")))

        draftDao.delete(routine)

        assertNull(draftDao.getByRoutine(routine))
        assertEquals(0, draftSetCount())
        assertEquals("Push", routineDao.getById(routine)!!.routine.name)
        assertEquals(1, database.query("SELECT * FROM exercises", null).use { it.count })
    }

    @Test
    fun deletingTheRoutineDeletesItsDraft() = runTest {
        val bench = insertExercise("Banca")
        val routine = insertRoutine("Push", listOf(bench))
        draftDao.save(WorkoutDraftEntity(routine, startedAt = 1), listOf(set(routine, 0, 0, bench, "60", "8")))

        routineDao.delete(routine)

        assertNull(draftDao.getByRoutine(routine))
        assertEquals(0, draftSetCount())
    }
}
