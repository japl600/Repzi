package com.japl.repzi.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Starts [intent] and returns false instead of crashing when no app can handle it. */
internal fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
