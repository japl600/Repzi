package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseSearchTest {

    private fun exercise(name: String) = Exercise(1, name, MuscleGroup.CHEST, isCustom = false)

    @Test
    fun aBlankQueryMatchesEverything() {
        assertTrue(exercise("Bench press").matchesSearch(""))
        assertTrue(exercise("Bench press").matchesSearch("   "))
    }

    @Test
    fun ignoresCase() {
        assertTrue(exercise("Bench press").matchesSearch("BENCH"))
        assertTrue(exercise("BENCH PRESS").matchesSearch("press"))
    }

    @Test
    fun ignoresAccentsInBothDirections() {
        assertTrue(exercise("Elevación lateral").matchesSearch("elevacion"))
        assertTrue(exercise("Elevacion lateral").matchesSearch("elevación"))
    }

    @Test
    fun matchesAnyPartOfTheName() {
        assertTrue(exercise("Incline dumbbell press").matchesSearch("dumbbell"))
        assertTrue(exercise("Incline dumbbell press").matchesSearch("bell pr"))
    }

    @Test
    fun everyWordHasToAppearInAnyOrder() {
        val exercise = exercise("Incline dumbbell press")

        assertTrue(exercise.matchesSearch("press incline"))
        assertFalse(exercise.matchesSearch("press barbell"))
    }

    @Test
    fun ignoresSurroundingAndRepeatedSpaces() {
        assertTrue(exercise("Bench press").matchesSearch("  bench    press "))
    }

    @Test
    fun doesNotMatchUnrelatedNames() {
        assertFalse(exercise("Bench press").matchesSearch("squat"))
    }
}
