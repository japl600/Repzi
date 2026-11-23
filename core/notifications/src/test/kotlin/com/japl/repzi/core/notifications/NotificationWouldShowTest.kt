package com.japl.repzi.core.notifications

import android.app.NotificationManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The decision behind [canPostNotifications], on plain values. The system cannot be made to block a test's own app
 * (revoking the permission kills the process), so each way of being blocked is covered here instead.
 */
class NotificationWouldShowTest {

    private val default = NotificationManager.IMPORTANCE_DEFAULT

    @Test
    fun aNotificationIsShownWhenNothingBlocksIt() {
        assertTrue(notificationWouldShow(systemEnabled = true, permitted = true, channelImportance = default))
    }

    @Test
    fun notificationsTurnedOffForTheAppInTheSystemBlockIt() {
        assertFalse(notificationWouldShow(systemEnabled = false, permitted = true, channelImportance = default))
    }

    @Test
    fun aMissingRuntimePermissionBlocksIt() {
        assertFalse(notificationWouldShow(systemEnabled = true, permitted = false, channelImportance = default))
    }

    @Test
    fun aChannelThatDoesNotExistBlocksIt() {
        assertFalse(notificationWouldShow(systemEnabled = true, permitted = true, channelImportance = null))
    }

    @Test
    fun aChannelTheUserSwitchedOffBlocksIt() {
        assertFalse(
            notificationWouldShow(systemEnabled = true, permitted = true, channelImportance = NotificationManager.IMPORTANCE_NONE),
        )
    }

    @Test
    fun aQuietChannelStillShowsTheNotification() {
        assertTrue(
            notificationWouldShow(systemEnabled = true, permitted = true, channelImportance = NotificationManager.IMPORTANCE_LOW),
        )
    }
}
