package com.japl.repzi.feature.routines

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class RoutineEditorScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val row = Exercise(3, "Remo", MuscleGroup.BACK, isCustom = false)

    private val loaded = RoutineEditorUiState(
        isLoading = false,
        isNew = false,
        name = "Full body",
        exercises = listOf(squat, bench),
    )

    private val names = mutableListOf<String>()
    private val toggled = mutableListOf<Exercise>()
    private val removed = mutableListOf<Int>()
    private val movedUp = mutableListOf<Int>()
    private val movedDown = mutableListOf<Int>()
    private var saved = 0
    private var back = 0

    private fun setScreen(
        uiState: RoutineEditorUiState = loaded,
        availableExercises: List<Exercise> = listOf(squat, bench, row),
    ) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                RoutineEditorScreen(
                    uiState = uiState,
                    availableExercises = availableExercises,
                    onBack = { back++ },
                    onNameChange = { names += it },
                    onExerciseToggled = { toggled += it },
                    onExerciseRemoved = { removed += it },
                    onExerciseMovedUp = { movedUp += it },
                    onExerciseMovedDown = { movedDown += it },
                    onSave = { saved++ },
                )
            }
        }
    }

    private fun string(@StringRes id: Int) = rule.activity.getString(id)

    @Test
    fun theTitleTellsANewRoutineFromAnEditedOne() {
        setScreen(loaded.copy(isNew = true))

        rule.onNodeWithText(string(R.string.editor_title_new)).assertIsDisplayed()
    }

    @Test
    fun anExistingRoutineShowsTheEditTitleAndItsNameAndExercises() {
        setScreen()

        rule.onNodeWithText(string(R.string.editor_title_edit)).assertIsDisplayed()
        rule.onNodeWithText("Full body").assertIsDisplayed()
        rule.onNodeWithText("Sentadilla").assertIsDisplayed()
        rule.onNodeWithText("Press de banca").assertIsDisplayed()
    }

    @Test
    fun savingIsDisabledWhileLoading() {
        setScreen(RoutineEditorUiState(isLoading = true))

        rule.onNodeWithText(string(R.string.editor_save)).assertIsNotEnabled()
    }

    @Test
    fun typingANameReportsIt() {
        setScreen(loaded.copy(name = ""))

        rule.onNode(hasSetTextAction()).performTextInput("Push")

        assertEquals(listOf("Push"), names)
    }

    @Test
    fun showsTheBlankNameError() {
        setScreen(loaded.copy(name = "", error = RoutineEditorError.BlankName))

        rule.onNodeWithText(string(R.string.editor_error_blank_name)).assertIsDisplayed()
    }

    @Test
    fun showsTheNoExercisesError() {
        setScreen(loaded.copy(exercises = emptyList(), error = RoutineEditorError.NoExercises))

        rule.onNodeWithText(string(R.string.editor_error_no_exercises)).assertIsDisplayed()
    }

    @Test
    fun theFirstExerciseCannotMoveUpAndTheLastCannotMoveDown() {
        setScreen()

        val up = rule.onAllNodesWithContentDescription(string(R.string.editor_move_up))
        val down = rule.onAllNodesWithContentDescription(string(R.string.editor_move_down))
        up[0].assertIsNotEnabled()
        up[1].assertIsEnabled()
        down[0].assertIsEnabled()
        down[1].assertIsNotEnabled()
    }

    @Test
    fun moveAndRemoveButtonsReportTheExercisePosition() {
        setScreen()

        rule.onAllNodesWithContentDescription(string(R.string.editor_move_up))[1].performClick()
        rule.onAllNodesWithContentDescription(string(R.string.editor_move_down))[0].performClick()
        rule.onAllNodesWithContentDescription(string(R.string.editor_remove))[1].performClick()

        assertEquals(listOf(1), movedUp)
        assertEquals(listOf(0), movedDown)
        assertEquals(listOf(1), removed)
    }

    @Test
    fun saveAndBackButtonsReportTheClick() {
        setScreen()

        rule.onNodeWithText(string(R.string.editor_save)).performClick()
        rule.onNodeWithContentDescription(string(R.string.editor_back)).performClick()

        assertEquals(1, saved)
        assertEquals(1, back)
    }

    @Test
    fun thePickerListsTheAvailableExercisesAndReportsTheOneTapped() {
        setScreen()
        // "Remo" is not in the routine yet, so it only appears once the picker is open.
        rule.onNodeWithText("Remo").assertDoesNotExist()

        rule.onNodeWithText(string(R.string.editor_add_exercises)).performClick()
        rule.onNodeWithText("Remo").assertIsDisplayed()
        rule.onNodeWithText("Remo").performClick()

        assertEquals(listOf(row), toggled)
    }

    @Test
    fun theDoneButtonClosesThePicker() {
        setScreen()
        rule.onNodeWithText(string(R.string.editor_add_exercises)).performClick()
        rule.onNodeWithText(string(R.string.editor_picker_done)).assertIsDisplayed()

        rule.onNodeWithText(string(R.string.editor_picker_done)).performClick()

        rule.onNodeWithText("Remo").assertDoesNotExist()
        rule.onNodeWithText(string(R.string.editor_picker_done)).assertDoesNotExist()
    }
}
