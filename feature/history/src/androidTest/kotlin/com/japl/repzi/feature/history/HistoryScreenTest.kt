package com.japl.repzi.feature.history

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
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.Rule

class HistoryScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private val fullBody = WorkoutSession(
        id = 1,
        routineName = "Full body",
        startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
        finishedAt = Instant.fromEpochMilliseconds(1_700_000_000_000 + 45 * 60_000),
        exercises = listOf(LoggedExercise(squat, listOf(LoggedSet(5, 100.0), LoggedSet(5, 102.5)))),
    )
    private val legs = fullBody.copy(id = 2, routineName = "Piernas")

    private val deleteRequested = mutableListOf<WorkoutSession>()
    private var deleteConfirmed = 0
    private var deleteDismissed = 0

    private fun setScreen(uiState: HistoryUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                HistoryScreen(
                    uiState = uiState,
                    onDeleteRequested = { deleteRequested += it },
                    onDeleteConfirmed = { deleteConfirmed++ },
                    onDeleteDismissed = { deleteDismissed++ },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun showsAHintWhenThereAreNoSessions() {
        setScreen(HistoryUiState(isLoading = false))

        rule.onNodeWithText(string(R.string.history_empty)).assertIsDisplayed()
    }

    @Test
    fun showsTheDurationVolumeAndSetsOfASession() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody)))

        val minutes = rule.activity.resources.getQuantityString(R.plurals.history_minutes, 45, 45)
        // 5 x 100 + 5 x 102.5 = 1012.5 kg, shown with its decimal part.
        rule.onNodeWithText("Full body").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.history_summary, minutes, "1012.5")).assertIsDisplayed()
        rule.onNodeWithText("Sentadilla").assertIsDisplayed()
        rule.onNodeWithText("100 kg × 5 · 102.5 kg × 5").assertIsDisplayed()
    }

    @Test
    fun showsEverySessionInTheGivenOrder() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody, legs)))

        rule.onNodeWithText("Full body").assertIsDisplayed()
        rule.onNodeWithText("Piernas").assertIsDisplayed()
        rule.onAllNodesWithContentDescription(string(R.string.history_delete)).assertCountEquals(2)
    }

    @Test
    fun theDeleteButtonRequestsThatSession() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody, legs)))

        rule.onAllNodesWithContentDescription(string(R.string.history_delete))[1].performClick()

        assertEquals(listOf(legs), deleteRequested)
    }

    @Test
    fun noDialogIsShownUntilADeletionIsPending() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody)))

        rule.onNodeWithText(string(R.string.history_delete_title)).assertDoesNotExist()
    }

    @Test
    fun confirmingTheDialogSendsTheConfirmation() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody), sessionToDelete = fullBody))

        rule.onNodeWithText(string(R.string.history_delete_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.history_delete_message, "Full body")).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.history_delete)).performClick()

        assertEquals(1, deleteConfirmed)
        assertEquals(0, deleteDismissed)
    }

    @Test
    fun cancellingTheDialogSendsTheDismissal() {
        setScreen(HistoryUiState(isLoading = false, sessions = listOf(fullBody), sessionToDelete = fullBody))

        rule.onNodeWithText(string(R.string.history_cancel)).performClick()

        assertEquals(1, deleteDismissed)
        assertEquals(0, deleteConfirmed)
    }
}
