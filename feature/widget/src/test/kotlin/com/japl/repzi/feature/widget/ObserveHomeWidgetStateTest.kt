package com.japl.repzi.feature.widget

import app.cash.turbine.test
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import com.japl.repzi.core.domain.usecase.ObserveWeightUnitUseCase
import com.japl.repzi.core.domain.usecase.ObserveWorkoutDraftsUseCase
import com.japl.repzi.core.testing.FakeDraftRepository
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ObserveHomeWidgetStateTest {

    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }

    private val push = Routine(id = 1, name = "Push", exercises = emptyList())
    private val pull = Routine(id = 2, name = "Pull", exercises = emptyList())

    private val sessions = FakeSessionRepository()
    private val routines = FakeRoutineRepository(listOf(pull, push))
    private val drafts = FakeDraftRepository()
    private val settings = FakeSettingsRepository()

    private fun observe() = ObserveHomeWidgetState(
        ObserveSessionsUseCase(sessions),
        ObserveRoutinesUseCase(routines),
        ObserveWorkoutDraftsUseCase(drafts),
        ObserveWeightUnitUseCase(settings),
        clock,
    )

    @Test
    fun theStateIsComputedAgainWhenAnyInputChanges() = runTest {
        observe()().test {
            assertEquals(WidgetTarget.Start(2, "Pull"), awaitItem().target)

            sessions.saveSession(
                WorkoutSession(routineName = "Pull", startedAt = now - 3.hours, finishedAt = now - 2.hours, exercises = emptyList()),
            )
            val afterTheSession = awaitItem()
            assertEquals("Pull", afterTheSession.lastWorkout?.routineName)
            assertEquals(WidgetTarget.Start(1, "Push"), afterTheSession.target)

            drafts.saveDraft(WorkoutDraft(routineId = 2, startedAt = now - 1.hours, exercises = emptyList()))
            assertEquals(WidgetTarget.Resume(2, "Pull"), awaitItem().target)

            drafts.deleteDraft(2)
            assertEquals(WidgetTarget.Start(1, "Push"), awaitItem().target)

            settings.setWeightUnit(WeightUnit.LB)
            assertEquals(WeightUnit.LB, awaitItem().weightUnit)

            routines.deleteRoutine(1)
            assertEquals(WidgetTarget.Start(2, "Pull"), awaitItem().target)
        }
    }
}
