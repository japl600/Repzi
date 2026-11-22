package com.japl.repzi.feature.widget

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HomeWidgetStateTest {

    private val now = Instant.parse("2026-11-22T12:00:00Z")

    private fun routine(id: Long, name: String) = Routine(id = id, name = name, exercises = emptyList())

    private fun session(
        routineName: String,
        finishedAt: Instant = now - 48.hours,
        duration: kotlin.time.Duration = 52.minutes,
        sets: List<LoggedSet> = emptyList(),
    ) = WorkoutSession(
        id = 1,
        routineName = routineName,
        startedAt = finishedAt - duration,
        finishedAt = finishedAt,
        exercises = listOf(LoggedExercise(Exercise(1, "Bench press", MuscleGroup.CHEST, isCustom = false), sets)),
    )

    private fun state(
        sessions: List<WorkoutSession> = emptyList(),
        routines: List<Routine> = emptyList(),
        drafts: List<WorkoutDraftInfo> = emptyList(),
        weightUnit: WeightUnit = WeightUnit.KG,
    ) = homeWidgetState(sessions, routines, drafts, weightUnit, now)

    private val legs = routine(1, "Legs")
    private val pull = routine(2, "Pull")
    private val push = routine(3, "Push")

    @Test
    fun noRoutinesGivesNoRoutinesButStillShowsTheLastWorkout() {
        val result = state(sessions = listOf(session("Push")))

        assertEquals(WidgetTarget.NoRoutines, result.target)
        assertEquals("Push", result.lastWorkout?.routineName)
    }

    @Test
    fun noSessionsStartsTheFirstRoutine() {
        val result = state(routines = listOf(pull, push))

        assertEquals(WidgetTarget.Start(2, "Pull"), result.target)
        assertNull(result.lastWorkout)
    }

    @Test
    fun startsTheRoutineAfterTheLastOneAndWrapsAround() {
        val routines = listOf(legs, pull, push)

        assertEquals(WidgetTarget.Start(1, "Legs"), state(listOf(session("Push")), routines).target)
        assertEquals(WidgetTarget.Start(3, "Push"), state(listOf(session("Pull")), routines).target)
    }

    @Test
    fun theLastSessionNameIsMatchedIgnoringCase() {
        val result = state(listOf(session("pull")), listOf(legs, pull, push))

        assertEquals(WidgetTarget.Start(3, "Push"), result.target)
    }

    @Test
    fun anUnknownLastRoutineStartsTheFirstOne() {
        val result = state(listOf(session("Old name")), listOf(legs, pull, push))

        assertEquals(WidgetTarget.Start(1, "Legs"), result.target)
    }

    @Test
    fun twoRoutinesWithTheSameNameUseTheFirstMatch() {
        val routines = listOf(routine(1, "A"), routine(2, "B"), routine(3, "B"), routine(4, "C"))

        val result = state(listOf(session("B")), routines)

        assertEquals(WidgetTarget.Start(3, "B"), result.target)
    }

    @Test
    fun aSingleRoutineIsItsOwnNext() {
        val result = state(listOf(session("Push")), listOf(push))

        assertEquals(WidgetTarget.Start(3, "Push"), result.target)
    }

    @Test
    fun aRecentDraftWinsOverTheRotationAndResumes() {
        val draft = WorkoutDraftInfo(routineId = 2, startedAt = now - 1.hours)

        val result = state(listOf(session("Push")), listOf(legs, pull, push), listOf(draft))

        assertEquals(WidgetTarget.Resume(2, "Pull"), result.target)
        assertEquals("Push", result.lastWorkout?.routineName)
    }

    @Test
    fun aDraftExactlySixHoursOldStillCountsAndOlderOnesDoNot() {
        val routines = listOf(legs, pull)
        val atTheLimit = WorkoutDraftInfo(2, now - GetWorkoutDraftUseCase.MAX_AGE)
        val tooOld = WorkoutDraftInfo(2, now - GetWorkoutDraftUseCase.MAX_AGE - 1.minutes)

        assertEquals(WidgetTarget.Resume(2, "Pull"), state(routines = routines, drafts = listOf(atTheLimit)).target)
        assertEquals(WidgetTarget.Start(1, "Legs"), state(routines = routines, drafts = listOf(tooOld)).target)
    }

    @Test
    fun theMostRecentDraftThatStillHasARoutineIsResumed() {
        val routines = listOf(legs, pull) // routine 3 was deleted
        val drafts = listOf(
            WorkoutDraftInfo(3, now - 1.hours),
            WorkoutDraftInfo(1, now - 3.hours),
            WorkoutDraftInfo(2, now - 2.hours),
        )

        assertEquals(WidgetTarget.Resume(2, "Pull"), state(routines = routines, drafts = drafts).target)
    }

    @Test
    fun aDraftOfADeletedRoutineIsIgnored() {
        val result = state(routines = listOf(legs), drafts = listOf(WorkoutDraftInfo(9, now - 1.hours)))

        assertEquals(WidgetTarget.Start(1, "Legs"), result.target)
    }

    @Test
    fun theLastWorkoutCarriesItsNumbersAndTheUnitIsKept() {
        val sets = listOf(LoggedSet(reps = 5, weightKg = 100.0), LoggedSet(reps = 8, weightKg = 50.0))

        val result = state(listOf(session("Push", sets = sets)), listOf(push), weightUnit = WeightUnit.LB)

        assertEquals(WeightUnit.LB, result.weightUnit)
        assertEquals(900.0, result.lastWorkout?.volumeKg)
        assertEquals(52.minutes, result.lastWorkout?.duration)
        assertEquals(now - 48.hours, result.lastWorkout?.finishedAt)
    }

    @Test
    fun daysAgoCountsCalendarDaysInTheGivenZone() {
        val utc = ZoneId.of("UTC")
        val noon = Instant.parse("2026-11-22T12:00:00Z")

        assertEquals(0, daysAgo(Instant.parse("2026-11-22T00:00:00Z"), noon, utc))
        assertEquals(1, daysAgo(Instant.parse("2026-11-21T23:59:00Z"), Instant.parse("2026-11-22T00:01:00Z"), utc))
        assertEquals(3, daysAgo(Instant.parse("2026-11-19T20:00:00Z"), noon, utc))
    }

    @Test
    fun daysAgoUsesTheDatesOfTheZoneNotOfUtc() {
        // 03:00 UTC on the 22nd is still the 21st in Lima (UTC-5), so from the 22nd at noon it is one day ago there.
        val from = Instant.parse("2026-11-22T03:00:00Z")
        val to = Instant.parse("2026-11-22T17:00:00Z")

        assertEquals(0, daysAgo(from, to, ZoneId.of("UTC")))
        assertEquals(1, daysAgo(from, to, ZoneId.of("America/Lima")))
    }

    @Test
    fun daysAgoOfAFutureMomentIsZero() {
        assertEquals(0, daysAgo(Instant.parse("2026-11-23T10:00:00Z"), Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC")))
    }
}
