package com.japl.repzi.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry

/** Shared helpers of the instrumented notification tests. */
internal object NotificationTestSupport {

    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    val manager: NotificationManager get() = context.getSystemService(NotificationManager::class.java)

    fun grantPostPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Notifications are posted asynchronously, so wait a little for the expected one. */
    fun activeWithId(id: Int): StatusBarNotification? {
        repeat(20) {
            manager.activeNotifications.firstOrNull { it.id == id }?.let { return it }
            Thread.sleep(100)
        }
        return null
    }

    /** Gives a notification that must NOT appear the time it would need to, then checks. */
    fun noneWithId(id: Int): Boolean {
        Thread.sleep(1_000)
        return manager.activeNotifications.none { it.id == id }
    }

    fun clear() = manager.cancelAll()
}
