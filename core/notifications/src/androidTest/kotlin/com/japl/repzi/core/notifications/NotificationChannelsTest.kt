package com.japl.repzi.core.notifications

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelsTest {

    private val context = NotificationTestSupport.context
    private val manager = NotificationTestSupport.manager

    @Test
    fun createsTheRestChannelWithHighImportanceAndVibration() {
        NotificationChannels.create(context)

        val channel = assertNotNull(manager.getNotificationChannel(NotificationChannels.REST))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(channel.shouldVibrate())
    }

    @Test
    fun createsTheInactivityChannelWithDefaultImportance() {
        NotificationChannels.create(context)

        val channel = assertNotNull(manager.getNotificationChannel(NotificationChannels.INACTIVITY))
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun creatingTwiceChangesNothing() {
        NotificationChannels.create(context)
        val before = manager.notificationChannels.size

        NotificationChannels.create(context)

        assertEquals(before, manager.notificationChannels.size)
    }
}
