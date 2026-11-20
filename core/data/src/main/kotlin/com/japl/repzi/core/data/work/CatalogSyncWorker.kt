package com.japl.repzi.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.japl.repzi.core.data.sync.CatalogSyncRunner
import com.japl.repzi.core.network.WgerException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the catalog sync. A failure that can pass (no network, server trouble) asks WorkManager to try again with
 * backoff; one that cannot (an unexpected response) fails this run, and the next weekly run tries again.
 */
@HiltWorker
internal class CatalogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: CatalogSyncRunner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        sync()
        Result.success()
    } catch (e: WgerException) {
        if (e.retryable) Result.retry() else Result.failure()
    }
}
