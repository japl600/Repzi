package com.japl.repzi.core.notifications

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.japl.repzi.core.domain.AppVisibility
import javax.inject.Inject

/** The app counts as visible while any of its activities is started (the process lifecycle). */
internal class ProcessAppVisibility @Inject constructor() : AppVisibility {

    override fun isVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
