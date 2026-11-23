package com.japl.repzi.feature.widget

import android.content.Context
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class WidgetTextsTest {

    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private val utc = ZoneId.of("UTC")

    private fun context(language: String = "en"): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply { setLocale(Locale(language)) }
        return base.createConfigurationContext(configuration)
    }

    private fun lastWorkout(daysAgo: Int = 2) = LastWorkout("Push", now - daysAgo.days)

    private fun state(
        lastWorkout: LastWorkout? = lastWorkout(),
        target: WidgetTarget = WidgetTarget.Start(2, "Pull"),
    ) = HomeWidgetState(lastWorkout, target)

    private fun texts(state: HomeWidgetState, language: String = "en") = widgetTexts(context(language), state, now, utc)

    @Test
    fun withNoWorkoutsAndNoRoutinesItInvitesToCreateOne() {
        val result = texts(state(lastWorkout = null, target = WidgetTarget.NoRoutines))

        assertEquals("No workouts yet", result.line)
        assertEquals("Create a routine", result.button)
    }

    @Test
    fun theLineShowsTheLastRoutineAndHowLongAgo() {
        val result = texts(state())

        assertEquals("Push · 2 days ago", result.line)
        assertEquals("Start", result.button)
    }

    @Test
    fun aWorkoutInProgressKeepsTheStartButtonAndSaysSoInTheLine() {
        val result = texts(state(target = WidgetTarget.Resume(2, "Pull")))

        assertEquals("Start", result.button)
        assertEquals("Pull - In progress", result.line)
    }

    @Test
    fun aWorkoutInProgressSaysSoEvenWithNoFinishedWorkout() {
        val result = texts(state(lastWorkout = null, target = WidgetTarget.Resume(2, "Pull")))

        assertEquals("Pull - In progress", result.line)
    }

    @Test
    fun theLastWorkoutStaysVisibleWhenThereAreNoRoutines() {
        val result = texts(state(target = WidgetTarget.NoRoutines))

        assertEquals("Push · 2 days ago", result.line)
        assertEquals("Create a routine", result.button)
    }

    @Test
    fun todayYesterdayAndSeveralDaysAgo() {
        assertEquals("Push · Today", texts(state(lastWorkout = lastWorkout(daysAgo = 0))).line)
        assertEquals("Push · Yesterday", texts(state(lastWorkout = lastWorkout(daysAgo = 1))).line)
        assertEquals("Push · 3 days ago", texts(state(lastWorkout = lastWorkout(daysAgo = 3))).line)
    }

    @Test
    fun theTextsFollowTheLanguage() {
        val last = texts(state(lastWorkout = lastWorkout(daysAgo = 3)), "es")
        assertEquals("Push · hace 3 días", last.line)
        assertEquals("Empezar", last.button)

        val resuming = texts(state(target = WidgetTarget.Resume(2, "Pull")), "es")
        assertEquals("Empezar", resuming.button)
        assertEquals("Pull - En curso", resuming.line)
        assertEquals("Push · Ayer", texts(state(lastWorkout = lastWorkout(daysAgo = 1)), "es").line)

        val empty = texts(state(lastWorkout = null, target = WidgetTarget.NoRoutines), "es")
        assertEquals("Aún sin entrenamientos", empty.line)
        assertEquals("Crear rutina", empty.button)
    }
}
