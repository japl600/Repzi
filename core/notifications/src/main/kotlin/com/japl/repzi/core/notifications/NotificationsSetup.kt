package com.japl.repzi.core.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Called once from the application's `onCreate`. Safe to call on every start. */
class NotificationsSetup @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun start() {
        NotificationChannels.create(context)
    }
}
