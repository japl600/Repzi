package com.japl.repzi.feature.routines

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class RoutinesScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private val fullBody = Routine(1, "Full body", listOf(squat, bench))
    private val legs = Routine(2, "Piernas", listOf(squat))

    private var created = 0
    private val opened = mutableListOf<Routine>()
    private val started = mutableListOf<Routine>()
    private val deleteRequested = mutableListOf<Routine>()
    private var deleteConfirmed = 0
    private var deleteDismissed = 0

    private fun setScreen(uiState: RoutinesUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                RoutinesScreen(
                    uiState = uiState,
                    onCreateRoutine = { created++ },
                    onOpenRoutine = { opened += it },
                    onStartRoutine = { started += it },
                    onDeleteRequested = { deleteRequested += it },
                    onDeleteConfirmed = { deleteConfirmed++ },
                    onDeleteDismissed = { deleteDismissed++ },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun exerciseCount(count: Int) =
        rule.activity.resources.getQuantityString(R.plurals.routines_exercise_count, count, count)

    @Test
    fun showsAHintWhenThereAreNoRoutines() {
        setScreen(RoutinesUiState(isLoading = false))

        rule.onNodeWithText(string(R.string.routines_empty)).assertIsDisplayed()
    }

    @Test
    fun showsEachRoutineWithItsExerciseCount() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody, legs)))

        rule.onNodeWithText("Full body").assertIsDisplayed()
        rule.onNodeWithText(exerciseCount(2)).assertIsDisplayed()
        rule.onNodeWithText("Piernas").assertIsDisplayed()
        rule.onNodeWithText(exerciseCount(1)).assertIsDisplayed()
    }

    @Test
    fun theCreateButtonAsksForANewRoutine() {
        setScreen(RoutinesUiState(isLoading = false))

        rule.onNodeWithContentDescription(string(R.string.routines_create)).performClick()

        assertEquals(1, created)
    }

    @Test
    fun tappingARoutineOpensIt() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody, legs)))

        rule.onNodeWithText("Piernas").performClick()

        assertEquals(listOf(legs), opened)
        assertEquals(emptyList(), started)
    }

    @Test
    fun theStartButtonStartsThatRoutineWithoutOpeningIt() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody, legs)))

        rule.onAllNodesWithContentDescription(string(R.string.routines_start))[1].performClick()

        assertEquals(listOf(legs), started)
        assertEquals(emptyList(), opened)
    }

    @Test
    fun theDeleteButtonRequestsThatRoutine() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody, legs)))

        rule.onAllNodesWithContentDescription(string(R.string.routines_delete)).assertCountEquals(2)
        rule.onAllNodesWithContentDescription(string(R.string.routines_delete))[0].performClick()

        assertEquals(listOf(fullBody), deleteRequested)
    }

    @Test
    fun noDialogIsShownUntilADeletionIsPending() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody)))

        rule.onNodeWithText(string(R.string.routines_delete_title)).assertDoesNotExist()
    }

    @Test
    fun confirmingTheDialogSendsTheConfirmation() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody), routineToDelete = fullBody))

        rule.onNodeWithText(string(R.string.routines_delete_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.routines_delete_message, "Full body")).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.routines_delete)).performClick()

        assertEquals(1, deleteConfirmed)
        assertEquals(0, deleteDismissed)
    }

    @Test
    fun cancellingTheDialogSendsTheDismissal() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody), routineToDelete = fullBody))

        rule.onNodeWithText(string(R.string.routines_cancel)).performClick()

        assertEquals(1, deleteDismissed)
        assertEquals(0, deleteConfirmed)
    }
}
