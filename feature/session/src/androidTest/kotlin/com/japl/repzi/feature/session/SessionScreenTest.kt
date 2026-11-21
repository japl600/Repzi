package com.japl.repzi.feature.session

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.R as UiR
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

/**
 * Drives the real screen with the real reducer, like the ViewModel does, and records every intent the screen sends.
 */
class SessionScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private val intents = mutableListOf<SessionIntent>()

    private val loaded = SessionState(
        isLoading = false,
        routineName = "Full body",
        exercises = listOf(
            ExerciseLog(squat, listOf(SetInput())),
            ExerciseLog(bench, listOf(SetInput())),
        ),
    )

    private fun setScreen(initial: SessionState = loaded) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                var state by remember { mutableStateOf(initial) }
                SessionScreen(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = state.reduce(intent)
                    },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    /** Text fields in screen order: weight and reps of the first set, then of the second, and so on. */
    private fun field(index: Int) = rule.onAllNodes(hasSetTextAction())[index]

    private fun typeSet(weight: String, reps: String, firstFieldIndex: Int = 0) {
        field(firstFieldIndex).performTextInput(weight)
        field(firstFieldIndex + 1).performTextInput(reps)
    }

    @Test
    fun theWeightFieldIsLabelledWithTheUnit() {
        setScreen(loaded.copy(weightUnit = WeightUnit.LB))

        rule.onAllNodesWithText(string(com.japl.repzi.core.ui.R.string.unit_lb)).assertCountEquals(2)
        rule.onAllNodesWithText(string(com.japl.repzi.core.ui.R.string.unit_kg)).assertCountEquals(0)
    }

    @Test
    fun showsTheRoutineNameAndOneEmptySetPerExercise() {
        setScreen()

        rule.onNodeWithText("Full body").assertIsDisplayed()
        rule.onNodeWithText("Sentadilla").assertIsDisplayed()
        rule.onNodeWithText("Press de banca").assertIsDisplayed()
        rule.onAllNodesWithText(string(R.string.session_set_label, 1)).assertCountEquals(2)
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(4)
    }

    @Test
    fun typedWeightAndRepsAreShownInTheFields() {
        setScreen()

        typeSet(weight = "100", reps = "5")

        field(0).assert(hasText("100"))
        field(1).assert(hasText("5"))
    }

    @Test
    fun theWeightFieldNormalizesWhatIsTyped() {
        setScreen()

        // Letters are dropped and the decimal comma becomes a dot.
        field(0).performTextInput("8a0,5")

        field(0).assert(hasText("80.5"))
    }

    @Test
    fun addingASetCopiesThePreviousOne() {
        setScreen()
        typeSet(weight = "100", reps = "5")

        rule.onAllNodesWithText(string(R.string.session_add_set))[0].performClick()

        rule.onNodeWithText(string(R.string.session_set_label, 2)).assertIsDisplayed()
        field(2).assert(hasText("100"))
        field(3).assert(hasText("5"))
    }

    @Test
    fun removingASetDropsItsRow() {
        setScreen()
        rule.onAllNodesWithText(string(R.string.session_add_set))[0].performClick()
        rule.onNodeWithText(string(R.string.session_set_label, 2)).assertIsDisplayed()

        rule.onAllNodesWithContentDescription(string(R.string.session_remove_set))[0].performClick()

        rule.onNodeWithText(string(R.string.session_set_label, 2)).assertDoesNotExist()
    }

    @Test
    fun theOnlySetOfAnExerciseCannotBeRemoved() {
        setScreen()

        rule.onAllNodesWithContentDescription(string(R.string.session_remove_set)).assertAll(isNotEnabled())
    }

    @Test
    fun finishSendsTheFinishIntent() {
        setScreen()

        rule.onNodeWithText(string(R.string.session_finish)).performClick()

        assertEquals(listOf<SessionIntent>(SessionIntent.FinishClicked), intents)
    }

    @Test
    fun finishIsDisabledWhileSaving() {
        setScreen(loaded.copy(isSaving = true))

        rule.onNodeWithText(string(R.string.session_finish)).assertIsNotEnabled()
    }

    @Test
    fun showsAnErrorWhenNothingWasLogged() {
        setScreen(loaded.copy(error = SessionError.NothingLogged))

        rule.onNodeWithText(string(R.string.session_error_nothing_logged)).assertIsDisplayed()
    }

    @Test
    fun closingWithoutInputDoesNotAskForConfirmation() {
        setScreen()

        rule.onNodeWithContentDescription(string(R.string.session_close)).performClick()

        assertEquals(listOf<SessionIntent>(SessionIntent.BackClicked), intents)
        rule.onNodeWithText(string(R.string.session_discard_title)).assertDoesNotExist()
    }

    @Test
    fun closingWithInputAsksBeforeDiscardingAndKeepsTheSetsIfTheUserStays() {
        setScreen()
        typeSet(weight = "100", reps = "5")

        rule.onNodeWithContentDescription(string(R.string.session_close)).performClick()
        rule.onNodeWithText(string(R.string.session_discard_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.session_discard_keep)).performClick()

        rule.onNodeWithText(string(R.string.session_discard_title)).assertDoesNotExist()
        field(0).assert(hasText("100"))
    }

    @Test
    fun confirmingTheDiscardSendsTheIntent() {
        setScreen()
        typeSet(weight = "100", reps = "5")
        rule.onNodeWithContentDescription(string(R.string.session_close)).performClick()

        rule.onNodeWithText(string(R.string.session_discard_confirm)).performClick()

        assertEquals(SessionIntent.DiscardConfirmed, intents.last())
    }

    @Test
    fun tickingASetSendsTheIntentAndMarksIt() {
        setScreen()

        rule.onAllNodesWithContentDescription(string(R.string.session_set_done, 1))[0].performClick()

        assertEquals(SessionIntent.SetDoneToggled(exerciseIndex = 0, setIndex = 0), intents.last())
        rule.onAllNodesWithContentDescription(string(R.string.session_set_done, 1))[0].assertIsOn()
        rule.onAllNodesWithContentDescription(string(R.string.session_set_done, 1))[1].assertIsOff()
    }

    @Test
    fun noCountdownBarWhileNotResting() {
        setScreen()

        rule.onNodeWithText(string(R.string.session_rest_skip)).assertDoesNotExist()
    }

    @Test
    fun theCountdownBarShowsTheTimeLeft() {
        setScreen(loaded.copy(rest = RestCountdown(totalSeconds = 60, remainingSeconds = 45)))

        rule.onNodeWithText("0:45").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.session_rest_title)).assertIsDisplayed()
    }

    @Test
    fun theCountdownButtonsMoveByFifteenSecondsWhileThereIsTimeLeft() {
        setScreen(loaded.copy(rest = RestCountdown(60, 45)))

        rule.onNodeWithContentDescription(string(R.string.session_rest_more_description, 15)).performClick()
        rule.onNodeWithContentDescription(string(R.string.session_rest_less_description, 15)).performClick()

        assertEquals(
            listOf(SessionIntent.RestAdjusted(15), SessionIntent.RestAdjusted(-15)),
            intents.filterIsInstance<SessionIntent.RestAdjusted>(),
        )
    }

    @Test
    fun theCountdownButtonsMoveByFiveSecondsNearTheEnd() {
        setScreen(loaded.copy(rest = RestCountdown(60, 20)))

        rule.onNodeWithContentDescription(string(R.string.session_rest_more_description, 5)).performClick()

        assertEquals(SessionIntent.RestAdjusted(5), intents.last())
        rule.onNodeWithText(string(R.string.session_rest_less, 5)).assertIsDisplayed()
    }

    @Test
    fun skippingSendsTheIntent() {
        setScreen(loaded.copy(rest = RestCountdown(60, 45)))

        rule.onNodeWithText(string(R.string.session_rest_skip)).performClick()

        assertEquals(SessionIntent.RestSkipped, intents.last())
    }

    @Test
    fun theMenuOfAnExerciseOpensTheRestPicker() {
        setScreen()

        rule.onNodeWithContentDescription(string(R.string.session_exercise_menu, "Sentadilla")).performClick()
        rule.onNodeWithText(string(R.string.session_edit_rest)).performClick()

        assertEquals(SessionIntent.RestEditRequested(0), intents.last())
        rule.onNodeWithText(string(UiR.string.rest_picker_title)).assertIsDisplayed()
    }

    @Test
    fun choosingARestInThePickerSendsItForThatExercise() {
        setScreen()
        rule.onNodeWithContentDescription(string(R.string.session_exercise_menu, "Sentadilla")).performClick()
        rule.onNodeWithText(string(R.string.session_edit_rest)).performClick()

        rule.onNodeWithText("0:10").performClick()
        rule.onNodeWithText(string(UiR.string.rest_picker_save)).performClick()

        assertEquals(SessionIntent.RestChanged(exerciseIndex = 0, seconds = 10), intents.last())
        rule.onNodeWithText(string(UiR.string.rest_picker_title)).assertDoesNotExist()
    }

    @Test
    fun theRestPlannedAfterEachSetIsShownBetweenTheSets() {
        setScreen(
            loaded.copy(
                exercises = listOf(
                    ExerciseLog(squat, listOf(SetInput(), SetInput(), SetInput()), restSeconds = 90),
                    ExerciseLog(bench, listOf(SetInput(), SetInput())),
                ),
            ),
        )

        // Three sets have two gaps; the exercise without a rest timer shows none.
        rule.onAllNodesWithText("1:30").assertCountEquals(2)
    }
}
