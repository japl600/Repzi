package com.japl.repzi.feature.settings

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Rule

class SettingsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var languageOpened = 0
    private var sourceCodeOpened = 0

    private fun setScreen(languageAvailable: Boolean = true) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                SettingsScreen(
                    versionLabel = "0.1.0 (1)",
                    languageAvailable = languageAvailable,
                    onOpenLanguage = { languageOpened++ },
                    onOpenSourceCode = { sourceCodeOpened++ },
                )
            }
        }
    }

    private fun string(@StringRes id: Int) = rule.activity.getString(id)

    @Test
    fun showsVersion() {
        setScreen()

        rule.onNodeWithText("0.1.0 (1)").assertIsDisplayed()
    }

    @Test
    fun languageRowIsShownWhenAvailable() {
        setScreen(languageAvailable = true)

        rule.onNodeWithText(string(R.string.settings_language)).performClick()

        assertEquals(1, languageOpened)
    }

    @Test
    fun languageRowIsHiddenWhenNotAvailable() {
        setScreen(languageAvailable = false)

        rule.onNodeWithText(string(R.string.settings_language)).assertDoesNotExist()
    }

    @Test
    fun sourceCodeRowOpensTheRepository() {
        setScreen()

        rule.onNodeWithText(string(R.string.settings_source_code)).performClick()

        assertEquals(1, sourceCodeOpened)
    }

    @Test
    fun startActivitySafelyReturnsFalseWithoutAHandler() {
        val opened = rule.activity.startActivitySafely(Intent("com.japl.repzi.NO_SUCH_ACTION"))

        assertFalse(opened)
    }
}
