package com.japl.repzi.core.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.domain.AppVisibility
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import com.japl.repzi.core.testing.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestReminderNotifierTest {

    private val support = NotificationTestSupport

    private fun notifier(
        settings: FakeSettingsRepository,
        visible: Boolean = false,
        gate: NotificationGate = SystemNotificationGate(support.context),
    ) = RestReminderNotifier(
        context = support.context,
        observeRestNotification = ObserveRestNotificationUseCase(settings),
        visibility = AppVisibility { visible },
        gate = gate,
    )

    @Before
    fun setUp() {
        support.grantPostPermission()
        NotificationChannels.create(support.context)
        support.clear()
    }

    @After
    fun tearDown() = support.clear()

    @Test
    fun postsWhenTheSettingIsOnAndTheAppIsInTheBackground() = runBlocking {
        notifier(FakeSettingsRepository(initialRestNotification = true)).onRestEnded()

        val posted = assertNotNull(support.activeWithId(RestReminderNotifier.NOTIFICATION_ID))
        assertEquals(NotificationChannels.REST, posted.notification.channelId)
        assertEquals(
            support.context.getString(R.string.notification_rest_title),
            posted.notification.extras.getString("android.title"),
        )
    }

    @Test
    fun postsNothingWhenTheSettingIsOffAtTheTimeOfTheAlarm() = runBlocking {
        // The alarm was scheduled while the setting was on; the user turned it off before it fired.
        val settings = FakeSettingsRepository(initialRestNotification = true)
        val notifier = notifier(settings)
        settings.setRestNotification(false)

        notifier.onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun postsNothingWhenTheAppIsVisible() = runBlocking {
        notifier(FakeSettingsRepository(initialRestNotification = true), visible = true).onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun postsNothingWhenNotificationsAreBlocked() = runBlocking {
        notifier(FakeSettingsRepository(initialRestNotification = true), gate = { false }).onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun theSystemGateAllowsNotificationsOnceThePermissionIsGranted() {
        assertTrue(SystemNotificationGate(support.context).canPost())
    }
}
