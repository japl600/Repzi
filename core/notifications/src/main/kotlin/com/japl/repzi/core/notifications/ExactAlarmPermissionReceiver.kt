package com.japl.repzi.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The system sends this when the user allows exact alarms for the app. A rest that was running got an approximate alarm
 * when it started; this sets it again so it comes out exact.
 */
internal class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AlarmManagerRestReminderScheduler(context.applicationContext).rescheduleIfPending()
    }
}
