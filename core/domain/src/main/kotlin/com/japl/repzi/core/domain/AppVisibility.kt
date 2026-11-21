package com.japl.repzi.core.domain

/**
 * Whether the user is looking at the app. A notification adds nothing then, so it is not posted, and a rest alarm that
 * is no longer needed can be dropped. Features never see how it is known.
 */
fun interface AppVisibility {
    fun isVisible(): Boolean
}
