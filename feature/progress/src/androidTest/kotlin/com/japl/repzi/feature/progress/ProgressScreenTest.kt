package com.japl.repzi.feature.progress

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

    private fun point(dayNumber: Int, kg: Double, reps: Int) = ProgressPoint(
        date = day(dayNumber),
        maxWeightKg = kg,
        reps = reps,
        volumeKg = kg * reps,
        estimatedOneRepMaxKg = kg * (1 + reps / 30.0),
        maxReps = reps,
    )

    private val points = listOf(
        point(0, 60.0, 5),
        point(7, 62.5, 5),
        point(14, 65.0, 4),
    )

    private var backs = 0
    private val exercisesSelected = mutableListOf<Long>()
    private val rangesSelected = mutableListOf<ProgressRange>()
    private val metricsSelected = mutableListOf<ProgressMetric>()

    private fun setScreen(uiState: ProgressUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                ProgressScreen(
                    uiState = uiState,
                    onBack = { backs++ },
                    onExerciseSelected = { exercisesSelected += it },
                    onRangeSelected = { rangesSelected += it },
                    onMetricSelected = { metricsSelected += it },
                )
            }
        }
    }

    private val withData = ProgressUiState(
        isLoading = false,
        exercises = listOf(bench, squat),
        selected = bench,
        availableMetrics = ProgressMetric.entries,
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

        val description = string(
            R.string.progress_chart_description,
            "Press de banca",
            string(R.string.progress_metric_max_weight),
            string(R.string.progress_kg, "65"),
            string(R.string.progress_kg, "65"),
        )

        rule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun theMetricChipsMarkTheCurrentOneAndReportAChange() {
        setScreen(withData)

        rule.onNodeWithText(string(R.string.progress_metric_chip_max_weight)).assertIsSelected()
        rule.onNodeWithText(string(R.string.progress_metric_chip_volume)).performScrollTo().performClick()

        assertEquals(listOf(ProgressMetric.VOLUME), metricsSelected)
    }

    @Test
    fun anExerciseWithASingleMetricShowsNoMetricChips() {
        setScreen(withData.copy(metric = ProgressMetric.REPS, availableMetrics = listOf(ProgressMetric.REPS)))

        rule.onNodeWithText(string(R.string.progress_metric_reps)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.progress_metric_chip_volume)).assertDoesNotExist()
    }

    @Test
    fun volumeIsShownInKilograms() {
        setScreen(withData.copy(metric = ProgressMetric.VOLUME))

        rule.onNodeWithText(string(R.string.progress_metric_volume)).assertIsDisplayed()
        // 65 kg x 4 reps, the latest session.
        rule.onAllNodesWithText(string(R.string.progress_kg, "260")).assertCountEquals(2)
    }

    @Test
    fun repsAreShownWithTheirUnit() {
        setScreen(withData.copy(metric = ProgressMetric.REPS))

        // The latest session did 4 reps: it shows in the headline and in the list below.
        val fourReps = rule.activity.resources.getQuantityString(R.plurals.progress_reps, 4, "4")
        rule.onAllNodesWithText(fourReps).assertCountEquals(2)
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
