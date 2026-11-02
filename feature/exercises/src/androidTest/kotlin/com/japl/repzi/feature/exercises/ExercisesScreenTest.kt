package com.japl.repzi.feature.exercises

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun setScreen(uiState: ExercisesUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                ExercisesScreen(uiState = uiState, onGroupSelected = { selections += it })
            }
        }
    }

    private fun string(@StringRes id: Int) = rule.activity.getString(id)

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
}
