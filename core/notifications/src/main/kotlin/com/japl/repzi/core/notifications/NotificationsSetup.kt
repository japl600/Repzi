package com.japl.repzi.core.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Called once from the application's `onCreate`. Safe to call on every start. */
class NotificationsSetup @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val inactivityScheduler: InactivityScheduler,
) {
    fun start() {
        NotificationChannels.create(context)
        inactivityScheduler.schedule()
    }
}
