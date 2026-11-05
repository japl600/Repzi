package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.WorkoutDraft

interface DraftRepository {
    suspend fun getDraft(routineId: Long): WorkoutDraft?

    /** Replaces the draft of the routine, if there was one. */
    suspend fun saveDraft(draft: WorkoutDraft)

    suspend fun deleteDraft(routineId: Long)
}
