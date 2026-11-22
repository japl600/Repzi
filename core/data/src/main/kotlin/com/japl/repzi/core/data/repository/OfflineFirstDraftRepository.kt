package com.japl.repzi.core.data.repository

import com.japl.repzi.core.data.mapper.toDomain
import com.japl.repzi.core.data.mapper.toEntity
import com.japl.repzi.core.data.mapper.toInfo
import com.japl.repzi.core.data.mapper.toSetEntities
import com.japl.repzi.core.database.dao.WorkoutDraftDao
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import com.japl.repzi.core.domain.repository.DraftRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstDraftRepository @Inject constructor(
    private val dao: WorkoutDraftDao,
    private val dispatchers: DispatcherProvider,
) : DraftRepository {

    override suspend fun getDraft(routineId: Long): WorkoutDraft? = withContext(dispatchers.io) {
        dao.getByRoutine(routineId)?.toDomain()
    }

    // Room runs the query for a Flow off the main thread by itself, so no withContext here.
    override fun observeDrafts(): Flow<List<WorkoutDraftInfo>> =
        dao.observeAll().map { drafts -> drafts.map { it.toInfo() } }

    override suspend fun saveDraft(draft: WorkoutDraft) = withContext(dispatchers.io) {
        dao.save(draft.toEntity(), draft.toSetEntities())
    }

    override suspend fun deleteDraft(routineId: Long) = withContext(dispatchers.io) {
        dao.delete(routineId)
    }
}
