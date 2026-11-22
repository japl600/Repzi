package com.japl.repzi.feature.widget

import android.content.Context
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.japl.repzi.core.domain.model.WeightUnit
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WidgetTextsTest {

    private val now = Instant.parse("2026-11-22T12:00:00Z")
    private val utc = ZoneId.of("UTC")

    private fun context(language: String = "en"): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply { setLocale(Locale(language)) }
        return base.createConfigurationContext(configuration)
    }

    private fun lastWorkout(
        daysAgo: Int = 2,
        volumeKg: Double = 4_200.0,
    ) = LastWorkout("Push", now - daysAgo.days, 52.minutes, volumeKg)

    private fun state(
        lastWorkout: LastWorkout? = lastWorkout(),
        target: WidgetTarget = WidgetTarget.Start(2, "Pull"),
        unit: WeightUnit = WeightUnit.KG,
    ) = HomeWidgetState(lastWorkout, target, unit)

    private fun texts(state: HomeWidgetState, language: String = "en") = widgetTexts(context(language), state, now, utc)

    @Test
    fun withNoWorkoutsAndNoRoutinesItInvitesToCreateOne() {
        val result = texts(state(lastWorkout = null, target = WidgetTarget.NoRoutines))

        assertEquals("No workouts yet", result.title)
        assertNull(result.subtitle)
        assertNull(result.summary)
        assertEquals("Create a routine", result.button)
    }

    @Test
    fun theLastWorkoutShowsItsRoutineHowLongAgoItsDurationAndVolume() {
        val result = texts(state())

        assertEquals("Last workout", result.title)
        assertEquals("Push · 2 days ago", result.subtitle)
        assertEquals("52 min · 4200 kg", result.summary)
        assertEquals("Start Pull", result.button)
    }

    @Test
    fun aWorkoutInProgressOffersToResumeIt() {
        assertEquals("Resume Pull", texts(state(target = WidgetTarget.Resume(2, "Pull"))).button)
    }

    @Test
    fun theLastWorkoutStaysVisibleWhenThereAreNoRoutines() {
        val result = texts(state(target = WidgetTarget.NoRoutines))

        assertEquals("Last workout", result.title)
        assertEquals("Create a routine", result.button)
    }

    @Test
    fun aWorkoutWithoutWeightShowsNoVolume() {
        assertEquals("52 min", texts(state(lastWorkout = lastWorkout(volumeKg = 0.0))).summary)
    }

    @Test
    fun poundsConvertTheVolume() {
        val result = texts(state(lastWorkout = lastWorkout(volumeKg = 100.0), unit = WeightUnit.LB))

        assertEquals("52 min · 220.46 lb", result.summary)
    }

    @Test
    fun todayYesterdayAndSeveralDaysAgo() {
        assertEquals("Push · Today", texts(state(lastWorkout = lastWorkout(daysAgo = 0))).subtitle)
        assertEquals("Push · Yesterday", texts(state(lastWorkout = lastWorkout(daysAgo = 1))).subtitle)
        assertEquals("Push · 3 days ago", texts(state(lastWorkout = lastWorkout(daysAgo = 3))).subtitle)
    }

    @Test
    fun theTextsFollowTheLanguage() {
        val last = texts(state(lastWorkout = lastWorkout(daysAgo = 3)), "es")
        assertEquals("Último entrenamiento", last.title)
        assertEquals("Push · hace 3 días", last.subtitle)
        assertEquals("Empezar Pull", last.button)

        assertEquals("Reanudar Pull", texts(state(target = WidgetTarget.Resume(2, "Pull")), "es").button)
        assertEquals("Ayer", texts(state(lastWorkout = lastWorkout(daysAgo = 1)), "es").subtitle?.substringAfter("· "))

        val empty = texts(state(lastWorkout = null, target = WidgetTarget.NoRoutines), "es")
        assertEquals("Aún sin entrenamientos", empty.title)
        assertEquals("Crear rutina", empty.button)
    }
}
