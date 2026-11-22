package com.japl.repzi.core.data

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** The language code (`"en"`, `"es"`) the app is showing now, which follows the per-app language setting. */
internal interface AppLanguage {
    operator fun invoke(): String

    /** The language now, and again every time it changes while the app is running. */
    val changes: Flow<String>
}

internal class ContextAppLanguage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppLanguage {

    override fun invoke(): String = context.resources.configuration.locales[0].language

    override val changes: Flow<String> = callbackFlow {
        val callbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                trySend(newConfig.locales[0].language)
            }

            @Deprecated("Not used", ReplaceWith("Unit"))
            override fun onLowMemory() = Unit
        }
        context.registerComponentCallbacks(callbacks)
        trySend(invoke())
        awaitClose { context.unregisterComponentCallbacks(callbacks) }
    }.distinctUntilChanged()
}

/**
 * Maps what a query emits, and maps it again when the app language changes. Names are chosen while mapping, and Room
 * does not emit when only the language changes, so without this the old language would stay on screen.
 */
internal fun <T, R> Flow<T>.remappedOnLanguageChange(language: AppLanguage, transform: (T) -> R): Flow<R> =
    combine(this, language.changes) { value, _ -> transform(value) }
