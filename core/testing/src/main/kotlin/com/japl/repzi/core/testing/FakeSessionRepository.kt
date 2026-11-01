package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository

class FakeSessionRepository : SessionRepository {

    private val _saved = mutableListOf<WorkoutSession>()
    val saved: List<WorkoutSession> get() = _saved

    override suspend fun saveSession(session: WorkoutSession): Long {
        val id = _saved.size + 1L
        _saved += session.copy(id = id)
        return id
    }
}
