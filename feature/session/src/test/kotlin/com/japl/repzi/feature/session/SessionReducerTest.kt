package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionReducerTest {

    private val bench = Exercise(1, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val squat = Exercise(2, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private val initial = SessionState(
        isLoading = false,
        routineName = "Full body",
        exercises = listOf(
            ExerciseLog(bench, listOf(SetInput())),
            ExerciseLog(squat, listOf(SetInput())),
        ),
    )

    private fun SessionState.setsOf(exerciseIndex: Int) = exercises[exerciseIndex].sets

    @Test
    fun typingUpdatesOnlyTheTargetedSet() {
        val state = initial
            .reduce(SessionIntent.WeightChanged(exerciseIndex = 1, setIndex = 0, value = "100"))
            .reduce(SessionIntent.RepsChanged(exerciseIndex = 1, setIndex = 0, value = "5"))

        assertEquals(listOf(SetInput()), state.setsOf(0))
        assertEquals(listOf(SetInput(weight = "100", reps = "5")), state.setsOf(1))
    }

    @Test
    fun repsAcceptOnlyDigits() {
        val state = initial.reduce(SessionIntent.RepsChanged(0, 0, "1a2.5"))

        assertEquals("125", state.setsOf(0).single().reps)
    }

    @Test
    fun weightAcceptsOneDecimalSeparatorAndNormalizesTheComma() {
        val state = initial.reduce(SessionIntent.WeightChanged(0, 0, "62,5.5kg"))

        assertEquals("62.55", state.setsOf(0).single().weight)
    }

    @Test
    fun addedSetCopiesThePreviousOne() {
        val state = initial
            .reduce(SessionIntent.WeightChanged(0, 0, "60"))
            .reduce(SessionIntent.RepsChanged(0, 0, "8"))
            .reduce(SessionIntent.SetAdded(0))

        assertEquals(listOf(SetInput("60", "8"), SetInput("60", "8")), state.setsOf(0))
    }

    @Test
    fun aSetCanBeRemovedButTheLastOneStays() {
        val withThree = initial.reduce(SessionIntent.SetAdded(0)).reduce(SessionIntent.SetAdded(0))
            .reduce(SessionIntent.WeightChanged(0, 1, "50"))

        val afterRemoval = withThree.reduce(SessionIntent.SetRemoved(0, 0))
        assertEquals(2, afterRemoval.setsOf(0).size)
        assertEquals("50", afterRemoval.setsOf(0).first().weight)

        val single = initial.reduce(SessionIntent.SetRemoved(0, 0))
        assertEquals(1, single.setsOf(0).size)
    }

    @Test
    fun intentsForAnUnknownExerciseAreIgnored() {
        assertEquals(initial, initial.reduce(SessionIntent.SetAdded(9)))
        assertEquals(initial, initial.reduce(SessionIntent.RepsChanged(9, 0, "5")))
    }

    @Test
    fun editingClearsTheError() {
        val state = initial.copy(error = SessionError.NothingLogged).reduce(SessionIntent.RepsChanged(0, 0, "5"))

        assertNull(state.error)
    }

    @Test
    fun theTickOfASetCanBeToggled() {
        val ticked = initial.reduce(SessionIntent.SetDoneToggled(0, 0))
        assertTrue(ticked.setsOf(0).single().done)
        assertFalse(ticked.setsOf(1).single().done)

        assertFalse(ticked.reduce(SessionIntent.SetDoneToggled(0, 0)).setsOf(0).single().done)
    }

    @Test
    fun typingInATickedSetKeepsItTicked() {
        val state = initial
            .reduce(SessionIntent.SetDoneToggled(0, 0))
            .reduce(SessionIntent.RepsChanged(0, 0, "8"))

        assertEquals(SetInput(reps = "8", done = true), state.setsOf(0).single())
    }

    @Test
    fun anAddedSetCopiesWeightAndRepsButStartsUnticked() {
        val state = initial
            .reduce(SessionIntent.WeightChanged(0, 0, "60"))
            .reduce(SessionIntent.RepsChanged(0, 0, "8"))
            .reduce(SessionIntent.SetDoneToggled(0, 0))
            .reduce(SessionIntent.SetAdded(0))

        assertEquals(
            listOf(SetInput("60", "8", done = true), SetInput("60", "8", done = false)),
            state.setsOf(0),
        )
    }

    @Test
    fun theTickDoesNotCountAsTypedInput() {
        assertFalse(initial.reduce(SessionIntent.SetDoneToggled(0, 0)).hasInput)
    }

    @Test
    fun tickingAnUnknownSetIsIgnored() {
        assertEquals(initial, initial.reduce(SessionIntent.SetDoneToggled(9, 0)))
        assertEquals(initial, initial.reduce(SessionIntent.SetDoneToggled(0, 9)))
    }

    @Test
    fun theRestPickerOpensForOneExerciseAndCloses() {
        val open = initial.reduce(SessionIntent.RestEditRequested(1))
        assertEquals(1, open.restEditing)

        assertNull(open.reduce(SessionIntent.RestEditDismissed).restEditing)
        assertNull(initial.reduce(SessionIntent.RestEditRequested(9)).restEditing)
    }

    @Test
    fun choosingARestUpdatesThatExerciseAndClosesThePicker() {
        val state = initial
            .reduce(SessionIntent.RestEditRequested(1))
            .reduce(SessionIntent.RestChanged(1, 120))

        assertEquals(listOf<Int?>(null, 120), state.exercises.map { it.restSeconds })
        assertNull(state.restEditing)
    }

    @Test
    fun theAdjustStepIsFineWhenTheRestIsAboutToEnd() {
        assertEquals(5, restAdjustStep(10))
        assertEquals(5, restAdjustStep(30))
        assertEquals(15, restAdjustStep(31))
        assertEquals(15, restAdjustStep(180))
    }

    @Test
    fun theProgressOfARestGoesFromZeroToOne() {
        assertEquals(0f, RestCountdown(60, 60).progress)
        assertEquals(0.5f, RestCountdown(60, 30).progress)
        assertEquals(1f, RestCountdown(60, 0).progress)
        assertEquals(1f, RestCountdown(0, 0).progress)
    }

    @Test
    fun backOnlyAsksForConfirmationWhenSomethingWasTyped() {
        assertFalse(initial.reduce(SessionIntent.BackClicked).showDiscardDialog)

        val typed = initial.reduce(SessionIntent.RepsChanged(0, 0, "5"))
        val asked = typed.reduce(SessionIntent.BackClicked)
        assertTrue(asked.showDiscardDialog)
        assertFalse(asked.reduce(SessionIntent.DiscardDismissed).showDiscardDialog)
    }
}
