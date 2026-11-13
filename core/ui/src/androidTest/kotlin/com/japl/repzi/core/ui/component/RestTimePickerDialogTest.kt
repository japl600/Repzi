package com.japl.repzi.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.japl.repzi.core.ui.R
import com.japl.repzi.core.ui.theme.RepziTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class RestTimePickerDialogTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val options = (10..180 step 10).toList()
    private val confirmed = mutableListOf<Int?>()
    private var dismissed = 0

    private fun show(initialSeconds: Int?) {
        rule.setContent {
            RepziTheme(dynamicColor = false) {
                RestTimePickerDialog(
                    options = options,
                    initialSeconds = initialSeconds,
                    onConfirm = { confirmed += it },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    private fun string(id: Int) = rule.activity.getString(id)

    @Test
    fun opensWithTheCurrentRestInTheMiddleOfTheWheel() {
        show(initialSeconds = 90)

        rule.onNodeWithText("1:30").assertIsDisplayed()
        rule.onNodeWithText("1:20").assertIsDisplayed()
    }

    @Test
    fun savingWithoutTouchingTheWheelKeepsTheCurrentRest() {
        show(initialSeconds = 60)

        rule.onNodeWithText(string(R.string.rest_picker_save)).performClick()

        assertEquals(listOf<Int?>(60), confirmed)
    }

    @Test
    fun tappingAnEntryChoosesIt() {
        show(initialSeconds = 60)

        rule.onNodeWithText("1:20").performClick()
        rule.onNodeWithText(string(R.string.rest_picker_save)).performClick()

        assertEquals(listOf<Int?>(80), confirmed)
    }

    @Test
    fun theFirstEntryTurnsTheRestTimerOff() {
        show(initialSeconds = 10)

        rule.onNodeWithText(string(R.string.rest_picker_none)).performClick()
        rule.onNodeWithText(string(R.string.rest_picker_save)).performClick()

        assertEquals(listOf<Int?>(null), confirmed)
    }

    @Test
    fun aRoutineWithoutRestOpensOnTheFirstEntry() {
        show(initialSeconds = null)

        rule.onNodeWithText(string(R.string.rest_picker_save)).performClick()

        assertEquals(listOf<Int?>(null), confirmed)
    }

    @Test
    fun cancelingDoesNotReportAChoice() {
        show(initialSeconds = 60)

        rule.onNodeWithText("1:20").performClick()
        rule.onNodeWithText(string(R.string.rest_picker_cancel)).performClick()

        assertEquals(emptyList(), confirmed)
        assertEquals(1, dismissed)
    }
}
