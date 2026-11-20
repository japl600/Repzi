package com.japl.repzi.feature.exercises

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class ExerciseDetailScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0
    private val urlsOpened = mutableListOf<String>()

    private val catalogDetail = ExerciseDetail(
        id = 1,
        name = "Kettlebell swing",
        muscleGroup = MuscleGroup.CORE,
        isCustom = false,
        description = "<ol><li>Hold the kettlebell.</li><li>Swing it.</li></ol>",
        author = "deusinvictus",
        licenseName = "CC-BY-SA 4",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/deed.en",
        sourceUrl = "https://wger.de/en/exercise/9/view/",
    )

    private fun setScreen(uiState: ExerciseDetailUiState) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                ExerciseDetailScreen(
                    uiState = uiState,
                    onBack = { backs++ },
                    onOpenUrl = { urlsOpened += it },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun showsDescriptionAuthorLicenseAndSource() {
        setScreen(ExerciseDetailUiState(isLoading = false, detail = catalogDetail))

        rule.onNodeWithText("Hold the kettlebell.", substring = true).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.exercise_detail_author, "deusinvictus")).assertIsDisplayed()
        rule.onNodeWithText("CC-BY-SA 4", substring = true).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.exercise_detail_source)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.exercise_detail_note)).assertIsDisplayed()
    }

    @Test
    fun customExerciseHasNoAuthorBlock() {
        setScreen(
            ExerciseDetailUiState(
                isLoading = false,
                detail = ExerciseDetail(
                    id = 2, name = "Mi remo", muscleGroup = MuscleGroup.BACK, isCustom = true, description = null,
                    author = null, licenseName = null, licenseUrl = null, sourceUrl = null,
                ),
            ),
        )

        rule.onNodeWithText("Mi remo").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.exercise_detail_author, ""), substring = true).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.exercise_detail_source)).assertDoesNotExist()
        rule.onNodeWithText(string(R.string.exercise_detail_note)).assertDoesNotExist()
    }

    @Test
    fun missingLicenseHidesTheLicenseRow() {
        setScreen(
            ExerciseDetailUiState(isLoading = false, detail = catalogDetail.copy(licenseName = null, licenseUrl = null)),
        )

        rule.onNodeWithText(string(R.string.exercise_detail_license, ""), substring = true).assertDoesNotExist()
        // The rest of the attribution is still there.
        rule.onNodeWithText(string(R.string.exercise_detail_author, "deusinvictus")).assertIsDisplayed()
    }

    @Test
    fun licenseLinkOpensItsUrl() {
        setScreen(ExerciseDetailUiState(isLoading = false, detail = catalogDetail))

        rule.onNodeWithText("CC-BY-SA 4", substring = true).performClick()

        assertEquals(listOf("https://creativecommons.org/licenses/by-sa/4.0/deed.en"), urlsOpened)
    }

    @Test
    fun sourceLinkOpensTheExercisePage() {
        setScreen(ExerciseDetailUiState(isLoading = false, detail = catalogDetail))

        rule.onNodeWithText(string(R.string.exercise_detail_source)).performClick()

        assertEquals(listOf("https://wger.de/en/exercise/9/view/"), urlsOpened)
    }

    @Test
    fun backButtonCallsBack() {
        setScreen(ExerciseDetailUiState(isLoading = false, detail = catalogDetail))

        rule.onNodeWithContentDescription(string(R.string.exercise_detail_back)).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun anUnknownExerciseShowsTheNotFoundMessage() {
        setScreen(ExerciseDetailUiState(isLoading = false, detail = null))

        rule.onNodeWithText(string(R.string.exercise_detail_not_found)).assertIsDisplayed()
    }
}
