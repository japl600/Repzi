package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.repository.DraftRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Returns the unfinished workout of a routine, or null when there is none. A draft that is too old is a workout the
 * user walked away from, not one to resume: it is deleted instead of being offered again.
 */
class GetWorkoutDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(routineId: Long): WorkoutDraft? {
        val draft = repository.getDraft(routineId) ?: return null
        if (clock.now() - draft.startedAt > MAX_AGE) {
            repository.deleteDraft(routineId)
            return null
        }
        return draft
    }

    companion object {
        val MAX_AGE = 6.hours
    }
}

class SaveWorkoutDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(draft: WorkoutDraft) = repository.saveDraft(draft)
}

class DeleteWorkoutDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(routineId: Long) = repository.deleteDraft(routineId)
}
