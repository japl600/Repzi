package com.japl.repzi.core.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.japl.repzi.core.data.sync.CatalogSyncRunner
import com.japl.repzi.core.network.WgerException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogSyncWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun run(sync: CatalogSyncRunner): Result {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = CatalogSyncWorker(appContext, workerParameters, sync)
        }
        val worker = TestListenableWorkerBuilder<CatalogSyncWorker>(context).setWorkerFactory(factory).build()
        return runBlocking { worker.doWork() }
    }

    @Test
    fun workerSucceedsWhenTheSyncSucceeds() {
        assertEquals(Result.success(), run { 3 })
    }

    @Test
    fun workerRetriesWhenTheSyncFailsWithARetryableError() {
        assertEquals(Result.retry(), run { throw WgerException("offline", retryable = true) })
    }

    @Test
    fun workerFailsWhenTheSyncFailsWithANonRetryableError() {
        assertEquals(Result.failure(), run { throw WgerException("bad response", retryable = false) })
    }
}
