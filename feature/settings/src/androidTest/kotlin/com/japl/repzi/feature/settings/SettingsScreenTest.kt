package com.japl.repzi.feature.settings

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Rule

class SettingsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var languageOpened = 0
    private var sourceCodeOpened = 0
    private var licensesOpened = 0
    private var exerciseDataOpened = 0
    private val vibrationChanges = mutableListOf<Boolean>()
    private val weightUnitChanges = mutableListOf<WeightUnit>()
    private val restNotificationChanges = mutableListOf<Boolean>()
    private val inactivityReminderChanges = mutableListOf<Boolean>()
    private val inactivityDaysChanges = mutableListOf<Int>()
    private var notificationSettingsOpened = 0
    private var exactAlarmSettingsOpened = 0

    private fun setScreen(
        languageAvailable: Boolean = true,
        restVibration: Boolean = true,
        restNotification: Boolean = false,
        inactivityReminder: Boolean = false,
        inactivityDays: Int = 3,
        weightUnit: WeightUnit = WeightUnit.KG,
        hints: NotificationHints = NotificationHints(notificationsBlocked = false, exactTimingMissing = false),
    ) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState(
                        restVibration = restVibration,
                        restNotification = restNotification,
                        inactivityReminder = inactivityReminder,
                        inactivityDays = inactivityDays,
                        weightUnit = weightUnit,
                    ),
                    onRestVibrationChange = { vibrationChanges += it },
                    onWeightUnitChange = { weightUnitChanges += it },
                    hints = hints,
                    onRestNotificationChange = { restNotificationChanges += it },
                    onInactivityReminderChange = { inactivityReminderChanges += it },
                    onInactivityDaysChange = { inactivityDaysChanges += it },
                    onOpenNotificationSettings = { notificationSettingsOpened++ },
                    onOpenExactAlarmSettings = { exactAlarmSettingsOpened++ },
                    versionLabel = "0.1.0 (1)",
                    languageAvailable = languageAvailable,
                    onOpenLanguage = { languageOpened++ },
                    onOpenSourceCode = { sourceCodeOpened++ },
                    onOpenLicenses = { licensesOpened++ },
                    onOpenExerciseData = { exerciseDataOpened++ },
                )
            }
        }
    }

    private fun string(@StringRes id: Int) = rule.activity.getString(id)

    @Test
    fun showsVersion() {
        setScreen()

        // The screen scrolls and the version is at the bottom: on a small screen it is below the fold until scrolled to.
        rule.onNodeWithText("0.1.0 (1)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun languageRowIsShownWhenAvailable() {
        setScreen(languageAvailable = true)

        rule.onNodeWithText(string(R.string.settings_language)).performScrollTo().performClick()

        assertEquals(1, languageOpened)
    }

    @Test
    fun languageRowIsHiddenWhenNotAvailable() {
        setScreen(languageAvailable = false)

        rule.onNodeWithText(string(R.string.settings_language)).assertDoesNotExist()
    }

    @Test
    fun sourceCodeRowOpensTheRepository() {
        setScreen()

        rule.onNodeWithText(string(R.string.settings_source_code)).performScrollTo().performClick()

        assertEquals(1, sourceCodeOpened)
    }

    @Test
    fun licensesRowOpensTheLicensesScreen() {
        setScreen()

        rule.onNodeWithText(string(R.string.settings_licenses)).performScrollTo().performClick()

        assertEquals(1, licensesOpened)
    }

    @Test
    fun exerciseDataRowOpensTheSource() {
        setScreen()

        rule.onNodeWithText(string(R.string.settings_exercise_data)).performScrollTo().performClick()

        assertEquals(1, exerciseDataOpened)
    }

    // There are three switches now, so each one is found by its own title.
    private fun switchOf(@StringRes title: Int) = isToggleable() and hasText(string(title))

    private fun days(count: Int) = rule.activity.resources.getQuantityString(R.plurals.settings_inactivity_days, count, count)

    @Test
    fun vibrationSwitchReflectsTheState() {
        setScreen(restVibration = false)
        rule.onNode(switchOf(R.string.settings_rest_vibration)).assertIsOff()
    }

    @Test
    fun vibrationSwitchIsOnWhenEnabled() {
        setScreen(restVibration = true)
        rule.onNode(switchOf(R.string.settings_rest_vibration)).assertIsOn()
    }

    @Test
    fun notificationSwitchesReflectTheState() {
        setScreen(restNotification = true, inactivityReminder = false)

        rule.onNode(switchOf(R.string.settings_rest_notification)).assertIsOn()
        rule.onNode(switchOf(R.string.settings_inactivity_reminder)).assertIsOff()
    }

    @Test
    fun notificationSwitchesCallBackWithTheNewValue() {
        setScreen(restNotification = false, inactivityReminder = false)

        rule.onNodeWithText(string(R.string.settings_rest_notification)).performScrollTo().performClick()
        rule.onNodeWithText(string(R.string.settings_inactivity_reminder)).performScrollTo().performClick()

        assertEquals(listOf(true), restNotificationChanges)
        assertEquals(listOf(true), inactivityReminderChanges)
    }

    @Test
    fun stepperIsHiddenWhileTheInactivityReminderIsOff() {
        setScreen(inactivityReminder = false)

        rule.onNodeWithText(days(3)).assertDoesNotExist()
    }

    @Test
    fun stepperShowsTheDays() {
        setScreen(inactivityReminder = true, inactivityDays = 3)

        rule.onNodeWithText(days(3)).assertIsDisplayed()
    }

    @Test
    fun stepperLessAndMoreCallBackWithDaysMinusAndPlusOne() {
        setScreen(inactivityReminder = true, inactivityDays = 5)

        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_less)).performScrollTo().performClick()
        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_more)).performScrollTo().performClick()

        assertEquals(listOf(4, 6), inactivityDaysChanges)
    }

    @Test
    fun stepperLessIsDisabledAtOneDay() {
        setScreen(inactivityReminder = true, inactivityDays = 1)

        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_less)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_more)).assertIsEnabled()
    }

    @Test
    fun stepperMoreIsDisabledAtFourteenDays() {
        setScreen(inactivityReminder = true, inactivityDays = 14)

        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_more)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(string(R.string.settings_inactivity_days_less)).assertIsEnabled()
    }

    @Test
    fun blockedHintIsShownAndOpensTheSystemSettings() {
        setScreen(hints = NotificationHints(notificationsBlocked = true, exactTimingMissing = false))

        rule.onNodeWithText(string(R.string.settings_notifications_blocked)).performScrollTo().performClick()

        assertEquals(1, notificationSettingsOpened)
    }

    @Test
    fun blockedHintIsHiddenWhenNothingIsBlocked() {
        setScreen()

        rule.onNodeWithText(string(R.string.settings_notifications_blocked)).assertDoesNotExist()
    }

    @Test
    fun exactTimingHintIsShownAndOpensItsScreen() {
        setScreen(restNotification = true, hints = NotificationHints(notificationsBlocked = false, exactTimingMissing = true))

        rule.onNodeWithText(string(R.string.settings_exact_timing_missing)).performScrollTo().performClick()

        assertEquals(1, exactAlarmSettingsOpened)
    }

    @Test
    fun exactTimingHintIsHiddenWhenItIsNotMissing() {
        setScreen(restNotification = true)

        rule.onNodeWithText(string(R.string.settings_exact_timing_missing)).assertDoesNotExist()
    }

    @Test
    fun tappingTheVibrationRowReportsTheNewValue() {
        setScreen(restVibration = true)

        rule.onNodeWithText(string(R.string.settings_rest_vibration)).performScrollTo().performClick()

        assertEquals(listOf(false), vibrationChanges)
    }

    @Test
    fun theWeightUnitRowMarksTheCurrentUnit() {
        setScreen(weightUnit = WeightUnit.LB)

        rule.onNodeWithText(string(R.string.settings_weight_unit)).assertIsDisplayed()
        rule.onNodeWithText(string(com.japl.repzi.core.ui.R.string.unit_lb)).assertIsSelected()
        rule.onNodeWithText(string(com.japl.repzi.core.ui.R.string.unit_kg)).assertIsNotSelected()
    }

    @Test
    fun choosingAnotherWeightUnitReportsIt() {
        setScreen(weightUnit = WeightUnit.KG)

        rule.onNodeWithText(string(com.japl.repzi.core.ui.R.string.unit_lb)).performScrollTo().performClick()

        assertEquals(listOf(WeightUnit.LB), weightUnitChanges)
    }

    @Test
    fun startActivitySafelyReturnsFalseWithoutAHandler() {
        val opened = rule.activity.startActivitySafely(Intent("com.japl.repzi.NO_SUCH_ACTION"))

        assertFalse(opened)
    }
}
