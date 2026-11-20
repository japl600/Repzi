package com.japl.repzi.core.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.japl.repzi.core.domain.CatalogSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val CATALOG_SYNC_WORK_NAME = "catalog-sync"

/**
 * One unique periodic work: every 7 days, only on an unmetered network (a pass downloads about 2.3 MB). A periodic work
 * also runs as soon as its constraints are met the first time, so no separate first-run work is needed. `KEEP` leaves an
 * existing schedule alone, so calling this on every app start is cheap.
 */
internal class WorkManagerCatalogSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CatalogSyncScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(CATALOG_SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
