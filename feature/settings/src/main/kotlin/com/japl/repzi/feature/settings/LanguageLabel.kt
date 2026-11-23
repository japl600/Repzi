package com.japl.repzi.feature.settings

import java.util.Locale

/** The name of [locale]'s language written in that language, starting with a capital: "English", "Español". */
internal fun languageLabel(locale: Locale): String =
    locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
