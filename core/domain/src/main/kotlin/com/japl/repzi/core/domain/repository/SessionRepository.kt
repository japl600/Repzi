package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeSessions(): Flow<List<WorkoutSession>>

    /** Persists a finished session and returns its id. */
    suspend fun saveSession(session: WorkoutSession): Long

    suspend fun deleteSession(id: Long)
}
