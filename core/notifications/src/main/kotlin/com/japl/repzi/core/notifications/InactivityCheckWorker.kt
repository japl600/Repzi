package com.japl.repzi.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs the daily inactivity check. Nothing here can fail in a way a retry would fix, so the run always succeeds. */
@HiltWorker
internal class InactivityCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val check: InactivityCheck,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        check()
        return Result.success()
    }
}
