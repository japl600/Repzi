package com.japl.repzi.feature.progress

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.Rule

class ProgressScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val bench = Exercise(1, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val squat = Exercise(2, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private fun day(n: Int) = Instant.fromEpochMilliseconds(1_700_000_000_000 + n * 86_400_000L)

    private val points = listOf(
        ProgressPoint(day(0), 60.0, 5),
        ProgressPoint(day(7), 62.5, 5),
        ProgressPoint(day(14), 65.0, 4),
    )

    private var backs = 0
    private val exercisesSelected = mutableListOf<Long>()
    private val rangesSelected = mutableListOf<ProgressRange>()

    private fun setScreen(uiState: ProgressUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                ProgressScreen(
                    uiState = uiState,
                    onBack = { backs++ },
                    onExerciseSelected = { exercisesSelected += it },
                    onRangeSelected = { rangesSelected += it },
                )
            }
        }
    }

    private val withData = ProgressUiState(
        isLoading = false,
        exercises = listOf(bench, squat),
        selected = bench,
        points = points,
    )

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun showsAHintWhenThereIsNoProgress() {
        setScreen(ProgressUiState(isLoading = false))

        rule.onNodeWithText(string(R.string.progress_empty)).assertIsDisplayed()
    }

    @Test
    fun showsTheChangeAndTheSessionCount() {
        setScreen(withData)

        rule.onNodeWithText("Press de banca").assertIsDisplayed()
        // 60 to 65 kg is +5 kg, which is 8 % (8.3 rounded).
        rule.onNodeWithText(string(R.string.progress_kg, "+5")).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.progress_percent, "+8")).assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun describesTheChartForAccessibility() {
        setScreen(withData)

        rule.onNodeWithContentDescription(string(R.string.progress_chart_description, "Press de banca", "65", "65"))
            .assertIsDisplayed()
    }

    @Test
    fun listsTheLatestSessions() {
        setScreen(withData)

        // The list is at the bottom of a scrollable screen, so on small screens it starts off screen.
        rule.onNodeWithText(string(R.string.progress_set, "65", 4)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(string(R.string.progress_set, "60", 5)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSelectedRangeIsMarkedAndTappingAnotherReportsIt() {
        setScreen(withData.copy(range = ProgressRange.ALL))

        rule.onNodeWithText(string(R.string.progress_range_all)).assertIsSelected()
        rule.onNodeWithText(string(R.string.progress_range_month)).performClick()

        assertEquals(listOf(ProgressRange.MONTH), rangesSelected)
    }

    @Test
    fun saysSoWhenTheRangeHasNoSessions() {
        setScreen(withData.copy(range = ProgressRange.MONTH, points = emptyList()))

        rule.onNodeWithText(string(R.string.progress_no_data_in_range)).assertIsDisplayed()
    }

    @Test
    fun theExercisePickerListsTheTrackedExercises() {
        setScreen(withData)

        rule.onNodeWithText("Press de banca").performClick()
        rule.onNodeWithText("Sentadilla").performClick()

        assertEquals(listOf(2L), exercisesSelected)
    }

    @Test
    fun theBackArrowGoesBack() {
        setScreen(withData)

        rule.onNodeWithContentDescription(string(R.string.progress_back)).performClick()

        assertEquals(1, backs)
    }
}
