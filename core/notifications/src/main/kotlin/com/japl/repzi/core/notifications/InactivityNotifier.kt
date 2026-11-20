package com.japl.repzi.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Tells the user how many days have passed. Separate from [InactivityCheck] so the rule is tested without Android. */
internal fun interface InactivityNotifier {
    /** Returns whether the notification was really posted: a blocked app posts nothing, and nothing is recorded then. */
    fun notify(days: Int): Boolean
}

internal class NotificationManagerInactivityNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gate: NotificationGate,
) : InactivityNotifier {

    override fun notify(days: Int): Boolean {
        if (!gate.canPost()) return false
        val notification = Notification.Builder(context, NotificationChannels.INACTIVITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_inactivity_title))
            .setContentText(context.resources.getQuantityString(R.plurals.notification_inactivity_text, days, days))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .apply { context.appLaunchIntent()?.let(::setContentIntent) }
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        const val NOTIFICATION_ID = 2
    }
}
