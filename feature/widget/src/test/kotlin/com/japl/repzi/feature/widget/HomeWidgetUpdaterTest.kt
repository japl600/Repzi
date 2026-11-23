package com.japl.repzi.feature.widget

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import com.japl.repzi.core.domain.usecase.ObserveWorkoutDraftsUseCase
import com.japl.repzi.core.domain.model.DraftExercise
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.testing.FakeDraftRepository
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.FakeSessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class HomeWidgetUpdaterTest {

    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }

    private val sessions = FakeSessionRepository()
    private val drafts = FakeDraftRepository()
    private val routines = FakeRoutineRepository(listOf(Routine(id = 1, name = "Push", exercises = emptyList())))

    private class CountingRefresher(private val failFirst: Boolean = false) : WidgetRefresher {
        var refreshes = 0
        override suspend fun refresh() {
            refreshes++
            if (failFirst && refreshes == 1) error("the launcher is gone")
        }
    }

    private fun updater(refresher: WidgetRefresher) = HomeWidgetUpdater(
        ObserveHomeWidgetState(
            ObserveSessionsUseCase(sessions),
            ObserveRoutinesUseCase(routines),
            ObserveWorkoutDraftsUseCase(drafts),
            clock,
            WidgetLanguage { "en" },
        ),
        refresher,
    )

    private fun draft(weight: String) = WorkoutDraft(
        routineId = 1,
        startedAt = now - 1.hours,
        exercises = listOf(DraftExercise(exerciseId = 1, sets = listOf(DraftSet(weight = weight, reps = "5")))),
    )

    @Test
    fun theWidgetIsRefreshedOnceForTheFirstState() = runTest(UnconfinedTestDispatcher()) {
        val refresher = CountingRefresher()

        updater(refresher).start(backgroundScope)

        assertEquals(1, refresher.refreshes)
    }

    @Test
    fun finishingAWorkoutRefreshesTheWidget() = runTest(UnconfinedTestDispatcher()) {
        val refresher = CountingRefresher()
        updater(refresher).start(backgroundScope)

        sessions.saveSession(
            WorkoutSession(routineName = "Push", startedAt = now - 2.hours, finishedAt = now - 1.hours, exercises = emptyList()),
        )

        assertEquals(2, refresher.refreshes)
    }

    @Test
    fun aDraftEditThatChangesNothingTheWidgetShowsDoesNotRefresh() = runTest(UnconfinedTestDispatcher()) {
        val refresher = CountingRefresher()
        updater(refresher).start(backgroundScope)

        drafts.saveDraft(draft(weight = "60")) // the widget turns to "in progress": a visible change
        drafts.saveDraft(draft(weight = "62.5")) // typing in the workout: nothing the widget shows
        drafts.saveDraft(draft(weight = "65"))

        assertEquals(2, refresher.refreshes)
    }

    @Test
    fun aFailingRefreshDoesNotStopTheUpdates() = runTest(UnconfinedTestDispatcher()) {
        val refresher = CountingRefresher(failFirst = true)
        val failures = mutableListOf<Throwable>()
        val updater = updater(refresher).apply { onRefreshFailure = { failures += it } }

        updater.start(backgroundScope)
        drafts.saveDraft(draft(weight = "60"))

        assertEquals(2, refresher.refreshes)
        assertEquals(listOf("the launcher is gone"), failures.map { it.message })
    }
}
