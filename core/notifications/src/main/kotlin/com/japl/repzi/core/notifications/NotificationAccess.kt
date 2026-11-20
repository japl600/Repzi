package com.japl.repzi.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Whether a notification would actually be shown: not blocked in the system and, on Android 13+, permitted. */
internal fun Context.canPostNotifications(): Boolean {
    val manager = getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return false
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

/** Tapping a notification brings the app back; null if the package has no launcher activity. */
internal fun Context.appLaunchIntent(): PendingIntent? {
    val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
    return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}
