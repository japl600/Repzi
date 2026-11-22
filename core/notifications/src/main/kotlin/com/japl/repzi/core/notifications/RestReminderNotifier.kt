package com.japl.repzi.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.japl.repzi.core.domain.AppVisibility
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * What happens when the rest alarm fires. The decision lives here, and not in the receiver, so it can be tested
 * without a Hilt application. The setting is read now, not when the alarm was set, so turning it off in between works.
 */
internal class RestReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeRestNotification: ObserveRestNotificationUseCase,
    private val visibility: AppVisibility,
    private val gate: NotificationGate,
) {

    /**
     * The reminder is a convenience, so a failure (the setting cannot be read, the system refuses the notice) ends
     * here: the receiver runs this on a scope nobody watches, and an exception would take the whole process down.
     */
    suspend fun onRestEnded() {
        try {
            postIfDue()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "The rest notice could not be posted", e)
        }
    }

    private suspend fun postIfDue() {
        if (!observeRestNotification().first()) return
        // On screen the countdown and the buzz already say it.
        if (visibility.isVisible()) return
        if (!gate.canPost(NotificationChannels.REST)) return

        val notification = Notification.Builder(context, NotificationChannels.REST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_rest_title))
            .setContentText(context.getString(R.string.notification_rest_text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .apply { context.appLaunchIntent()?.let(::setContentIntent) }
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val TAG = "RestReminderNotifier"
    }
}
