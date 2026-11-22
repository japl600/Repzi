package com.japl.repzi.core.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether a notification sent to [channelId] would actually be shown. A seam because a test cannot block its own
 * app: revoking the permission makes Android kill the process.
 */
internal fun interface NotificationGate {
    fun canPost(channelId: String): Boolean
}

internal class SystemNotificationGate @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationGate {

    override fun canPost(channelId: String): Boolean = context.canPostNotifications(channelId)
}
