package com.japl.repzi.core.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Removes the app's notices from the panel once the app is on screen. Both say something the user has just seen by
 * opening the app (the rest is over, it is time to train), and tapping the notice is not the only way to get here: a
 * notice left behind would still say "rest over" in the middle of the next set.
 */
internal class StaleNotificationCleaner(private val context: Context) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(RestReminderNotifier.NOTIFICATION_ID)
        manager.cancel(NotificationManagerInactivityNotifier.NOTIFICATION_ID)
    }
}
