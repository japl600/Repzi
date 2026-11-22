package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import com.japl.repzi.core.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDraftRepository : DraftRepository {

    private val byRoutine = MutableStateFlow<Map<Long, WorkoutDraft>>(emptyMap())
    val drafts: Map<Long, WorkoutDraft> get() = byRoutine.value

    override suspend fun getDraft(routineId: Long): WorkoutDraft? = byRoutine.value[routineId]

    override fun observeDrafts(): Flow<List<WorkoutDraftInfo>> =
        byRoutine.map { drafts -> drafts.values.map { WorkoutDraftInfo(it.routineId, it.startedAt) } }

    override suspend fun saveDraft(draft: WorkoutDraft) {
        byRoutine.value += draft.routineId to draft
    }

    override suspend fun deleteDraft(routineId: Long) {
        byRoutine.value -= routineId
    }
}
