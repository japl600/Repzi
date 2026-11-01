package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.WorkoutSession

interface SessionRepository {
    /** Persists a finished session and returns its id. */
    suspend fun saveSession(session: WorkoutSession): Long
}
