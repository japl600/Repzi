package com.japl.repzi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.japl.repzi.core.domain.CatalogSyncScheduler
import com.japl.repzi.core.notifications.NotificationsSetup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RepziApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var catalogSyncScheduler: CatalogSyncScheduler

    @Inject lateinit var notificationsSetup: NotificationsSetup

    // WorkManager starts on demand with this configuration (its default initializer is removed in the manifest), so
    // workers can be built by Hilt.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        catalogSyncScheduler.schedule()
        notificationsSetup.start()
    }
}
