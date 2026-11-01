package com.japl.repzi.core.data.repository

import com.japl.repzi.core.data.mapper.toDomain
import com.japl.repzi.core.data.mapper.toEntity
import com.japl.repzi.core.data.mapper.toSetEntities
import com.japl.repzi.core.database.dao.WorkoutSessionDao
import com.japl.repzi.core.database.model.WorkoutSessionWithSets
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstSessionRepository @Inject constructor(
    private val dao: WorkoutSessionDao,
    private val dispatchers: DispatcherProvider,
) : SessionRepository {

    override fun observeSessions(): Flow<List<WorkoutSession>> =
        dao.observeAll().map { list -> list.map(WorkoutSessionWithSets::toDomain) }

    override suspend fun saveSession(session: WorkoutSession): Long = withContext(dispatchers.io) {
        dao.save(session.toEntity(), session.toSetEntities())
    }

    override suspend fun deleteSession(id: Long) = withContext(dispatchers.io) {
        dao.delete(id)
    }
}
