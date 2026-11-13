package com.japl.repzi.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RestTimerTest {

    @Test
    fun offersTenSecondStepsFromTenSecondsToThreeMinutes() {
        assertEquals(18, RestTimer.options.size)
        assertEquals(10, RestTimer.options.first())
        assertEquals(180, RestTimer.options.last())
        assertEquals(listOf(10, 20, 30), RestTimer.options.take(3))
    }

    @Test
    fun coreExercisesStartWithAShortRest() {
        assertEquals(20, RestTimer.defaultFor(MuscleGroup.CORE))
    }

    @Test
    fun everyOtherGroupStartsAtAMinute() {
        MuscleGroup.entries.filter { it != MuscleGroup.CORE }.forEach {
            assertEquals(60, RestTimer.defaultFor(it))
        }
    }

    @Test
    fun theDefaultsAreAmongTheOptions() {
        MuscleGroup.entries.forEach { assert(RestTimer.defaultFor(it) in RestTimer.options) }
    }
}
