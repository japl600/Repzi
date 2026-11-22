package com.japl.repzi.feature.widget

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Redraws the widgets. A seam so the updater can be tested without Glance. */
fun interface WidgetRefresher {
    suspend fun refresh()
}

/**
 * Keeps the home widget in step with the app: whenever what it shows changes it asks for a redraw. Typing in a workout
 * rewrites the draft all the time, but the widget only shows that a draft exists, so those writes produce an equal
 * state and are dropped by [distinctUntilChanged].
 */
@Singleton
class HomeWidgetUpdater @Inject constructor(
    private val observeState: ObserveHomeWidgetState,
    private val refresher: WidgetRefresher,
) {
    /** Where a failed redraw is reported; replaced in tests, which have no `Log`. */
    internal var onRefreshFailure: (Throwable) -> Unit = { Log.w(TAG, "Could not refresh the home widget", it) }

    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)): Job = scope.launch {
        observeState().distinctUntilChanged().collect {
            try {
                refresher.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A widget that cannot be redrawn must not stop the next updates.
                onRefreshFailure(e)
            }
        }
    }

    private companion object {
        const val TAG = "HomeWidgetUpdater"
    }
}
