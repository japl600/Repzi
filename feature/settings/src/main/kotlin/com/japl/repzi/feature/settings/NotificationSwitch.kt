package com.japl.repzi.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/** The switches that need the notification permission to be turned on. */
internal enum class NotificationSwitch { REST, INACTIVITY }

/**
 * The switch the user turned on while the permission dialog is open, applied only if they allow it. It is a plain value
 * and not an action, so it survives the activity being recreated (a rotation) while the dialog is up: the answer comes
 * back afterwards and still has to know which switch it was for.
 */
@Composable
internal fun rememberPendingNotificationSwitch(): MutableState<NotificationSwitch?> =
    rememberSaveable { mutableStateOf<NotificationSwitch?>(null) }

/** Whether the user said no to the permission, which is what shows the "blocked" hint. Kept across a rotation too. */
@Composable
internal fun rememberPermissionDenied(): MutableState<Boolean> = rememberSaveable { mutableStateOf(false) }

/** Turns the switch on once the permission is there. */
internal fun NotificationSwitch.turnOn(viewModel: SettingsViewModel) = when (this) {
    NotificationSwitch.REST -> viewModel.onRestNotificationChange(true)
    NotificationSwitch.INACTIVITY -> viewModel.onInactivityReminderChange(true)
}
