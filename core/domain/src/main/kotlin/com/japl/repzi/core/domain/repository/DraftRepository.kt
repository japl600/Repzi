package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    suspend fun getDraft(routineId: Long): WorkoutDraft?

    /** The unfinished workouts that exist, without their sets. Emits again whenever one is saved or deleted. */
    fun observeDrafts(): Flow<List<WorkoutDraftInfo>>

    /** Replaces the draft of the routine, if there was one. */
    suspend fun saveDraft(draft: WorkoutDraft)

    suspend fun deleteDraft(routineId: Long)
}
