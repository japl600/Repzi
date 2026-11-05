package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.repository.DraftRepository

class FakeDraftRepository : DraftRepository {

    private val byRoutine = mutableMapOf<Long, WorkoutDraft>()
    val drafts: Map<Long, WorkoutDraft> get() = byRoutine.toMap()

    override suspend fun getDraft(routineId: Long): WorkoutDraft? = byRoutine[routineId]

    override suspend fun saveDraft(draft: WorkoutDraft) {
        byRoutine[draft.routineId] = draft
    }

    override suspend fun deleteDraft(routineId: Long) {
        byRoutine.remove(routineId)
    }
}
