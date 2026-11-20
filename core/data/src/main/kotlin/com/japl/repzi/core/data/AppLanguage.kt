package com.japl.repzi.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The language code (`"en"`, `"es"`) the app is showing now, which follows the per-app language setting. */
internal fun interface AppLanguage {
    operator fun invoke(): String
}

internal class ContextAppLanguage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppLanguage {
    override fun invoke(): String = context.resources.configuration.locales[0].language
}
