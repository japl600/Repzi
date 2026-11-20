package com.japl.repzi.core.notifications

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject

/** Whether the user is looking at the app. A notification adds nothing then, so it is not posted. */
fun interface AppVisibility {
    fun isVisible(): Boolean
}

internal class ProcessAppVisibility @Inject constructor() : AppVisibility {

    override fun isVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
