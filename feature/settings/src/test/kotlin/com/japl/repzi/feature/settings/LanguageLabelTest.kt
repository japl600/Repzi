package com.japl.repzi.feature.settings

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageLabelTest {

    @Test
    fun aLanguageIsNamedInItself() {
        assertEquals("English", languageLabel(Locale.forLanguageTag("en")))
        assertEquals("Español", languageLabel(Locale.forLanguageTag("es")))
    }

    @Test
    fun theRegionDoesNotChangeTheName() {
        assertEquals("Español", languageLabel(Locale.forLanguageTag("es-PE")))
    }

    @Test
    fun aLanguageCapitalisesItsFirstLetterWhateverTheLanguageDoes() {
        // Spanish writes language names in lower case; the row is a label, so it starts with a capital.
        assertEquals("Français", languageLabel(Locale.forLanguageTag("fr")))
    }
}
