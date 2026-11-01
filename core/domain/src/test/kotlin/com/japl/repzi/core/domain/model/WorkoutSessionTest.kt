package com.japl.repzi.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WorkoutSessionTest {

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private val session = WorkoutSession(
        routineName = "Full body",
        startedAt = Instant.fromEpochMilliseconds(0),
        finishedAt = Instant.fromEpochMilliseconds(45 * 60_000L),
        exercises = listOf(
            LoggedExercise(squat, listOf(LoggedSet(5, 100.0), LoggedSet(5, 100.0))),
            LoggedExercise(bench, listOf(LoggedSet(10, 0.0), LoggedSet(8, 62.5))),
        ),
    )

    @Test
    fun durationIsTheTimeBetweenStartAndFinish() {
        assertEquals(45.minutes, session.duration)
    }

    @Test
    fun volumeSumsRepsTimesWeightOverEverySet() {
        assertEquals(1_500.0, session.totalVolumeKg)
    }
}
