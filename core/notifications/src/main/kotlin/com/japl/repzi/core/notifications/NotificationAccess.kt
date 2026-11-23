package com.japl.repzi.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Whether a notification sent to [channelId] would actually be shown: notifications are not blocked in the system, on
 * Android 13+ they are permitted, and the channel exists and has not been switched off by the user.
 */
internal fun Context.canPostNotifications(channelId: String): Boolean {
    val manager = getSystemService(NotificationManager::class.java)
    val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return notificationWouldShow(
        systemEnabled = manager.areNotificationsEnabled(),
        permitted = permitted,
        channelImportance = manager.getNotificationChannel(channelId)?.importance,
    )
}

/**
 * The decision on plain values, so every way of being blocked can be tested: [channelImportance] is null when the
 * channel does not exist.
 */
internal fun notificationWouldShow(systemEnabled: Boolean, permitted: Boolean, channelImportance: Int?): Boolean =
    systemEnabled && permitted && channelImportance != null && channelImportance != NotificationManager.IMPORTANCE_NONE

/** Tapping a notification brings the app back; null if the package has no launcher activity. */
internal fun Context.appLaunchIntent(): PendingIntent? {
    val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
    return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}
