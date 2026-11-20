package com.japl.repzi.feature.settings

import com.japl.repzi.core.domain.repository.DEFAULT_INACTIVITY_DAYS

data class SettingsUiState(
    val restVibration: Boolean = true,
    val restNotification: Boolean = false,
    val inactivityReminder: Boolean = false,
    val inactivityDays: Int = DEFAULT_INACTIVITY_DAYS,
)

/**
 * What the system says about the notifications, shown as tappable lines under the switches. They describe the phone,
 * not a stored setting, so the route works them out and refreshes them when the user comes back to the screen.
 */
internal data class NotificationHints(
    /** Notifications are off for the app (permission denied or blocked in system settings) although the user wants them. */
    val notificationsBlocked: Boolean,
    /** The rest alarm cannot be exact because the special access was not granted. */
    val exactTimingMissing: Boolean,
)
