package com.japl.repzi.feature.routines

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.PredefinedRoutine
import com.japl.repzi.core.domain.model.PredefinedRoutines
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.RoutineExercise
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class RoutinesScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private val fullBody = Routine(1, "Mi rutina", listOf(RoutineExercise(squat), RoutineExercise(bench)))
    private val legs = Routine(2, "Tren inferior", listOf(RoutineExercise(squat)))

    private var created = 0
    private val opened = mutableListOf<Routine>()
    private val started = mutableListOf<Routine>()
    private val deleteRequested = mutableListOf<Routine>()
    private var deleteConfirmed = 0
    private var deleteDismissed = 0
    private val added = mutableListOf<Pair<PredefinedRoutine, String>>()

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
                    onAddPredefined = { template, name -> added += template to name },
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

        rule.onNodeWithText("Mi rutina").assertIsDisplayed()
        // The predefined routines below also show counts, so the first match is the one of the user's routine.
        rule.onAllNodesWithText(exerciseCount(2)).onFirst().assertIsDisplayed()
        rule.onNodeWithText("Tren inferior").assertIsDisplayed()
        rule.onAllNodesWithText(exerciseCount(1)).onFirst().assertIsDisplayed()
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

        rule.onNodeWithText("Tren inferior").performClick()

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
        rule.onNodeWithText(string(R.string.routines_delete_message, "Mi rutina")).assertIsDisplayed()
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

    /** The predefined routines are below the user's own, so on a small screen they are not composed until scrolled to. */
    private fun scrollTo(text: String) {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun withoutRoutinesThePredefinedOnesAreStillOffered() {
        setScreen(RoutinesUiState(isLoading = false))

        scrollTo(string(R.string.predefined_push_name))

        rule.onNodeWithText(string(R.string.routines_predefined_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.predefined_push_name)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.predefined_push_description)).assertIsDisplayed()
    }

    @Test
    fun theUsersRoutinesGetTheirOwnTitle() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody)))

        rule.onNodeWithText(string(R.string.routines_my_title)).assertIsDisplayed()
    }

    @Test
    fun everyPredefinedRoutineIsListedWithItsExerciseCount() {
        setScreen(RoutinesUiState(isLoading = false))

        PredefinedRoutines.all.forEach { template ->
            val texts = PREDEFINED_ROUTINE_TEXTS.getValue(template.key)
            scrollTo(string(texts.name))
            rule.onNodeWithText(string(texts.name)).assertIsDisplayed()
        }
    }

    @Test
    fun theAddButtonSendsTheTemplateWithItsNameInTheCurrentLanguage() {
        setScreen(RoutinesUiState(isLoading = false, routines = listOf(fullBody)))
        val template = PredefinedRoutines.all.first { it.key == "push" }
        val name = string(R.string.predefined_push_name)

        scrollTo(name)
        rule.onNodeWithContentDescription(string(R.string.routines_predefined_add, name)).performClick()

        assertEquals(listOf(template to name), added)
        assertEquals(emptyList(), opened)
    }
}
