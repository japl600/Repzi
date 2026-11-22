package com.japl.repzi.core.notifications

import android.app.Notification
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaleNotificationCleanerTest {

    private val support = NotificationTestSupport
    private val context = support.context

    @Before
    fun setUp() {
        support.grantPostPermission()
        NotificationChannels.create(context)
        support.clear()
    }

    @After
    fun tearDown() = support.clear()

    private fun post(id: Int, channelId: String) {
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("stale")
            .build()
        support.manager.notify(id, notification)
        assertNotNull(support.activeWithId(id))
    }

    /** A lifecycle the test drives by hand, with the cleaner watching it the way it watches the process. */
    private fun appLifecycle(): LifecycleRegistry {
        val owner = object : LifecycleOwner {
            lateinit var registry: LifecycleRegistry
            override val lifecycle: Lifecycle get() = registry
        }
        return LifecycleRegistry.createUnsafe(owner).also { owner.registry = it }
    }

    @Test
    fun theRestNoticeIsRemovedWhenTheAppComesBackToTheScreen() {
        post(RestReminderNotifier.NOTIFICATION_ID, NotificationChannels.REST)
        val lifecycle = appLifecycle().also { it.addObserver(StaleNotificationCleaner(context)) }

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun theInactivityNoticeIsRemovedToo() {
        post(NotificationManagerInactivityNotifier.NOTIFICATION_ID, NotificationChannels.INACTIVITY)
        val lifecycle = appLifecycle().also { it.addObserver(StaleNotificationCleaner(context)) }

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertTrue(support.noneWithId(NotificationManagerInactivityNotifier.NOTIFICATION_ID))
    }
}
