package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository(initial: List<WorkoutSession> = emptyList()) : SessionRepository {

    private val sessions = MutableStateFlow(initial)
    val saved: List<WorkoutSession> get() = sessions.value

    override fun observeSessions(): Flow<List<WorkoutSession>> = sessions

    override suspend fun saveSession(session: WorkoutSession): Long {
        val id = (sessions.value.maxOfOrNull { it.id } ?: 0) + 1
        sessions.value += session.copy(id = id)
        return id
    }

    override suspend fun deleteSession(id: Long) {
        sessions.value = sessions.value.filterNot { it.id == id }
    }
}
