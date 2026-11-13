package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class SetExerciseRestUseCaseTest {

    private data class Change(val routineId: Long, val position: Int, val restSeconds: Int?)

    private class RecordingRepository : RoutineRepository {
        val changes = mutableListOf<Change>()

        override fun observeRoutines(): Flow<List<Routine>> = emptyFlow()
        override suspend fun getRoutine(id: Long): Routine? = null
        override suspend fun saveRoutine(routine: Routine): Long = routine.id
        override suspend fun setRestSeconds(routineId: Long, position: Int, restSeconds: Int?) {
            changes += Change(routineId, position, restSeconds)
        }
        override suspend fun deleteRoutine(id: Long) = Unit
    }

    private val repository = RecordingRepository()
    private val useCase = SetExerciseRestUseCase(repository)

    @Test
    fun storesOneOfTheAllowedDurations() = runTest {
        assertTrue(useCase(routineId = 4, position = 2, restSeconds = 90))

        assertEquals(listOf(Change(4, 2, 90)), repository.changes)
    }

    @Test
    fun nullTurnsTheRestTimerOff() = runTest {
        assertTrue(useCase(routineId = 4, position = 0, restSeconds = null))

        assertEquals(listOf(Change(4, 0, null)), repository.changes)
    }

    @Test
    fun rejectsDurationsOutsideTheOptions() = runTest {
        assertFalse(useCase(routineId = 4, position = 0, restSeconds = 5))
        assertFalse(useCase(routineId = 4, position = 0, restSeconds = 95))
        assertFalse(useCase(routineId = 4, position = 0, restSeconds = 600))

        assertTrue(repository.changes.isEmpty())
    }
}
