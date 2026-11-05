package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.DraftExercise
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.repository.DraftRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class GetWorkoutDraftUseCaseTest {

    private class FakeClock(var current: Instant) : Clock {
        override fun now() = current
    }

    private class InMemoryRepository(draft: WorkoutDraft? = null) : DraftRepository {
        var draft: WorkoutDraft? = draft

        override suspend fun getDraft(routineId: Long) = draft?.takeIf { it.routineId == routineId }
        override suspend fun saveDraft(draft: WorkoutDraft) {
            this.draft = draft
        }
        override suspend fun deleteDraft(routineId: Long) {
            if (draft?.routineId == routineId) draft = null
        }
    }

    private val startedAt = Instant.fromEpochMilliseconds(1_000_000)
    private val draft = WorkoutDraft(
        routineId = 5,
        startedAt = startedAt,
        exercises = listOf(DraftExercise(1, listOf(DraftSet("100", "5")))),
    )
    private val clock = FakeClock(startedAt)

    @Test
    fun returnsNothingWhenThereIsNoDraft() = runTest {
        assertNull(GetWorkoutDraftUseCase(InMemoryRepository(), clock)(5))
    }

    @Test
    fun returnsTheDraftOfThatRoutine() = runTest {
        clock.current = startedAt + 30.minutes
        val useCase = GetWorkoutDraftUseCase(InMemoryRepository(draft), clock)

        assertEquals(draft, useCase(5))
        assertNull(useCase(6))
    }

    @Test
    fun aDraftExactlyAtTheLimitIsStillResumed() = runTest {
        clock.current = startedAt + GetWorkoutDraftUseCase.MAX_AGE

        assertEquals(draft, GetWorkoutDraftUseCase(InMemoryRepository(draft), clock)(5))
    }

    @Test
    fun aTooOldDraftIsDiscardedAndDeleted() = runTest {
        val repository = InMemoryRepository(draft)
        clock.current = startedAt + GetWorkoutDraftUseCase.MAX_AGE + 1.minutes

        assertNull(GetWorkoutDraftUseCase(repository, clock)(5))
        assertNull(repository.draft)
    }
}
