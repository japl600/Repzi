package com.japl.repzi.core.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InactivityNotifierTest {

    private val support = NotificationTestSupport
    private val context = support.context

    private fun expectedText(days: Int) =
        context.resources.getQuantityString(R.plurals.notification_inactivity_text, days, days)

    @Before
    fun setUp() {
        support.grantPostPermission()
        NotificationChannels.create(context)
        support.clear()
    }

    @After
    fun tearDown() = support.clear()

    @Test
    fun postsOnTheInactivityChannelWithThePluralText() {
        val notifier = NotificationManagerInactivityNotifier(context, SystemNotificationGate(context))

        assertTrue(notifier.notify(5))

        val posted = assertNotNull(support.activeWithId(NotificationManagerInactivityNotifier.NOTIFICATION_ID))
        assertEquals(NotificationChannels.INACTIVITY, posted.notification.channelId)
        assertEquals(expectedText(5), posted.notification.extras.getString("android.text"))
        assertEquals(
            context.getString(R.string.notification_inactivity_title),
            posted.notification.extras.getString("android.title"),
        )
    }

    @Test
    fun theTextCountsTheDaysAndSingularDiffersFromPlural() {
        // Whatever the device language, one day and five days must not read the same.
        assertNotEquals(expectedText(1), expectedText(5))
    }

    @Test
    fun returnsFalseAndPostsNothingWhenNotificationsAreBlocked() {
        val notifier = NotificationManagerInactivityNotifier(context) { false }

        assertFalse(notifier.notify(5))

        assertTrue(support.noneWithId(NotificationManagerInactivityNotifier.NOTIFICATION_ID))
    }
}
