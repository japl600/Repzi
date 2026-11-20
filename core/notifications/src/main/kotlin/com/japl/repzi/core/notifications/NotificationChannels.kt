package com.japl.repzi.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

internal object NotificationChannels {
    const val REST = "rest"
    const val INACTIVITY = "inactivity"

    /** Safe to call on every app start: creating a channel that exists keeps what the user changed in it. */
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val rest = NotificationChannel(
            REST,
            context.getString(R.string.notification_channel_rest_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_rest_description)
            enableVibration(true)
        }
        val inactivity = NotificationChannel(
            INACTIVITY,
            context.getString(R.string.notification_channel_inactivity_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_inactivity_description)
        }
        manager.createNotificationChannels(listOf(rest, inactivity))
    }
}
