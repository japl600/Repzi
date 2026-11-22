package com.japl.repzi.feature.settings

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.Rule

class PendingNotificationSwitchTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSwitchWaitingForThePermissionSurvivesTheActivityBeingRecreated() {
        // Rotating the phone with the permission dialog open recreates the activity, and the user's answer arrives
        // afterwards: it has to still know which switch it was for.
        val tester = StateRestorationTester(rule)
        var pending: MutableState<NotificationSwitch?>? = null
        tester.setContent { pending = rememberPendingNotificationSwitch() }
        rule.runOnIdle { pending!!.value = NotificationSwitch.INACTIVITY }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertEquals(NotificationSwitch.INACTIVITY, pending!!.value) }
    }

    @Test
    fun nothingIsPendingAtFirst() {
        var pending: MutableState<NotificationSwitch?>? = null
        rule.setContent { pending = rememberPendingNotificationSwitch() }

        rule.runOnIdle { assertNull(pending!!.value) }
    }

    @Test
    fun aDeniedPermissionIsRememberedAcrossRecreation() {
        val tester = StateRestorationTester(rule)
        var denied: MutableState<Boolean>? = null
        tester.setContent { denied = rememberPermissionDenied() }
        rule.runOnIdle { assertFalse(denied!!.value) }
        rule.runOnIdle { denied!!.value = true }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertEquals(true, denied!!.value) }
    }
}
