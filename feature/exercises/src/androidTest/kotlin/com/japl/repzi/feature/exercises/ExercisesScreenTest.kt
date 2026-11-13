package com.japl.repzi.feature.exercises

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

class ExercisesScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val selections = mutableListOf<MuscleGroup?>()
    private val progressOpened = mutableListOf<Long>()
    private val queries = mutableListOf<String>()

    private fun setScreen(uiState: ExercisesUiState, searchQuery: String = "") {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                ExercisesScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { queries += it },
                    onGroupSelected = { selections += it },
                    onOpenProgress = { progressOpened += it },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun showsEachExerciseWithItsMuscleGroup() {
        setScreen(
            ExercisesUiState(
                isLoading = false,
                exercises = listOf(
                    Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false),
                    Exercise(2, "Mi remo", MuscleGroup.BACK, isCustom = true),
                ),
            ),
        )

        rule.onNodeWithText("Sentadilla").assertIsDisplayed()
        rule.onNodeWithText("Mi remo").assertIsDisplayed()
        // Custom exercises carry a badge next to the group.
        rule.onNodeWithText("${string(R.string.muscle_group_back)} · ${string(R.string.exercises_custom_badge)}")
            .assertIsDisplayed()
    }

    @Test
    fun showsAHintWhenTheFilterHasNoExercises() {
        setScreen(ExercisesUiState(isLoading = false, selectedGroup = MuscleGroup.CORE))

        rule.onNodeWithText(string(R.string.exercises_empty)).assertIsDisplayed()
    }

    @Test
    fun tappingAChipReportsTheGroupAndTappingAllClearsIt() {
        setScreen(ExercisesUiState(isLoading = false))

        rule.onNodeWithText(string(R.string.muscle_group_legs)).performClick()
        rule.onNodeWithText(string(R.string.exercises_filter_all)).performClick()

        assertEquals(listOf(MuscleGroup.LEGS, null), selections)
    }

    @Test
    fun onlyTheSelectedGroupChipIsMarkedAsSelected() {
        setScreen(ExercisesUiState(isLoading = false, selectedGroup = MuscleGroup.CHEST))

        rule.onNodeWithText(string(R.string.muscle_group_chest)).assertIsSelected()
    }

    @Test
    fun onlyExercisesWithProgressHaveAProgressButton() {
        setScreen(
            ExercisesUiState(
                isLoading = false,
                exercises = listOf(
                    Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false),
                    Exercise(2, "Plancha", MuscleGroup.CORE, isCustom = false),
                ),
                exercisesWithProgress = setOf(1),
            ),
        )

        rule.onNodeWithContentDescription(string(R.string.exercises_progress, "Sentadilla")).assertIsDisplayed()
        rule.onNodeWithContentDescription(string(R.string.exercises_progress, "Plancha")).assertDoesNotExist()
    }

    @Test
    fun theProgressButtonReportsItsExercise() {
        setScreen(
            ExercisesUiState(
                isLoading = false,
                exercises = listOf(Exercise(7, "Sentadilla", MuscleGroup.LEGS, isCustom = false)),
                exercisesWithProgress = setOf(7),
            ),
        )

        rule.onNodeWithContentDescription(string(R.string.exercises_progress, "Sentadilla")).performClick()

        assertEquals(listOf(7L), progressOpened)
    }

    @Test
    fun typingInTheSearchFieldReportsTheText() {
        setScreen(ExercisesUiState(isLoading = false))

        rule.onNodeWithText(string(R.string.exercises_search)).performTextInput("sent")

        assertEquals(listOf("sent"), queries)
    }

    @Test
    fun theClearButtonOnlyShowsWhileThereIsText() {
        setScreen(ExercisesUiState(isLoading = false))

        rule.onNodeWithContentDescription(string(R.string.exercises_search_clear)).assertDoesNotExist()
    }

    @Test
    fun theClearButtonEmptiesTheQuery() {
        setScreen(ExercisesUiState(isLoading = false), searchQuery = "sent")

        rule.onNodeWithContentDescription(string(R.string.exercises_search_clear)).performClick()

        assertEquals(listOf(""), queries)
    }

    @Test
    fun saysWhenNothingMatchesTheSearch() {
        setScreen(ExercisesUiState(isLoading = false), searchQuery = "  xyz ")

        rule.onNodeWithText(string(R.string.exercises_no_results, "xyz")).assertIsDisplayed()
    }
}
