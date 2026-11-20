package com.japl.repzi.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface RestReminderEntryPoint {
    fun notifier(): RestReminderNotifier
}

/**
 * Receives the rest alarm. A thin shell: [RestReminderNotifier] decides whether to post anything. It reaches Hilt
 * through an entry point because `@AndroidEntryPoint` needs `super.onReceive`, which Kotlin rejects as abstract.
 */
internal class RestReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifier = EntryPointAccessors
            .fromApplication(context.applicationContext, RestReminderEntryPoint::class.java)
            .notifier()
        // Reading the setting is asynchronous: goAsync keeps the process alive until the work is done.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                notifier.onRestEnded()
            } finally {
                pending.finish()
            }
        }
    }
}
