package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ObserveSessionsUseCaseTest {

    private class StaticRepository(private val sessions: List<WorkoutSession>) : SessionRepository {
        override fun observeSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override suspend fun saveSession(session: WorkoutSession): Long = 0
        override suspend fun deleteSession(id: Long) = Unit
    }

    private fun session(id: Long, startedAtMillis: Long) = WorkoutSession(
        id = id,
        routineName = "Routine $id",
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        finishedAt = Instant.fromEpochMilliseconds(startedAtMillis + 1_000),
        exercises = emptyList(),
    )

    @Test
    fun emitsTheMostRecentSessionFirst() = runTest {
        val repository = StaticRepository(listOf(session(1, 1_000), session(2, 9_000), session(3, 5_000)))

        val ids = ObserveSessionsUseCase(repository)().first().map { it.id }

        assertEquals(listOf(2L, 3L, 1L), ids)
    }
}
