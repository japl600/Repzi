package com.japl.repzi.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.domain.AppVisibility
import com.japl.repzi.core.domain.repository.SettingsRepository
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import com.japl.repzi.core.testing.FakeSettingsRepository
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    fun postsNothingWhenItsOwnChannelIsSilenced() = runBlocking {
        // The gate is asked about the channel this notification would go to, not about notifications in general.
        val gate = NotificationGate { channelId -> channelId != NotificationChannels.REST }

        notifier(FakeSettingsRepository(initialRestNotification = true), gate = gate).onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    /** A setting that cannot be read, as when the DataStore file is damaged or the disk fails. */
    private class UnreadableSettings : SettingsRepository by FakeSettingsRepository() {
        override val restNotification: Flow<Boolean> = flow { throw IOException("the settings file cannot be read") }
    }

    @Test
    fun aFailureReadingTheSettingIsSwallowedInsteadOfCrashingTheApp() = runBlocking {
        val notifier = RestReminderNotifier(
            context = support.context,
            observeRestNotification = ObserveRestNotificationUseCase(UnreadableSettings()),
            visibility = AppVisibility { false },
            gate = SystemNotificationGate(support.context),
        )

        // The receiver runs this on a scope nobody watches: an exception here would take the whole process down.
        notifier.onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun aFailurePostingTheNoticeIsSwallowedToo() = runBlocking {
        val gate = NotificationGate { throw SecurityException("the system refused the notification") }

        notifier(FakeSettingsRepository(initialRestNotification = true), gate = gate).onRestEnded()

        assertTrue(support.noneWithId(RestReminderNotifier.NOTIFICATION_ID))
    }

    @Test
    fun theSystemGateAllowsNotificationsOnceThePermissionIsGranted() {
        assertTrue(SystemNotificationGate(support.context).canPost(NotificationChannels.REST))
    }

    @Test
    fun theSystemGateRefusesASilencedChannel() {
        // A channel created with importance NONE is what the user gets by switching a channel off in the settings.
        val silenced = NotificationChannel("silenced_test", "Silenced", NotificationManager.IMPORTANCE_NONE)
        support.manager.createNotificationChannel(silenced)
        try {
            assertFalse(SystemNotificationGate(support.context).canPost("silenced_test"))
        } finally {
            support.manager.deleteNotificationChannel("silenced_test")
        }
    }

    @Test
    fun theSystemGateRefusesAChannelThatDoesNotExist() {
        assertFalse(SystemNotificationGate(support.context).canPost("no_such_channel"))
    }
}
