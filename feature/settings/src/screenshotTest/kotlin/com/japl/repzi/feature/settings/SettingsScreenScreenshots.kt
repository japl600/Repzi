package com.japl.repzi.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.japl.repzi.core.ui.theme.RepziTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsDefaultsLight() {
    RepziTheme(darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(),
            onRestVibrationChange = {},
            onWeightUnitChange = {},
            hints = NotificationHints(notificationsBlocked = false, exactTimingMissing = false),
            onRestNotificationChange = {},
            onInactivityReminderChange = {},
            onInactivityDaysChange = {},
            onOpenNotificationSettings = {},
            onOpenExactAlarmSettings = {},
            versionLabel = "0.1.0 (1)",
            languageAvailable = true,
            currentLanguage = "English",
            onOpenLanguage = {},
            onOpenSourceCode = {},
            onOpenExerciseData = {},
            onOpenLicenses = {},
        )
    }
}
