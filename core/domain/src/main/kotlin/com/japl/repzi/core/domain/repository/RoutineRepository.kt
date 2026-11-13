package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>

    suspend fun getRoutine(id: Long): Routine?

    /** Inserts the routine when its id is [Routine.NEW_ID], replaces it otherwise. Returns the saved id. */
    suspend fun saveRoutine(routine: Routine): Long

    /** Changes the rest of the exercise at [position] of the routine, null meaning no rest timer. */
    suspend fun setRestSeconds(routineId: Long, position: Int, restSeconds: Int?)

    suspend fun deleteRoutine(id: Long)
}
