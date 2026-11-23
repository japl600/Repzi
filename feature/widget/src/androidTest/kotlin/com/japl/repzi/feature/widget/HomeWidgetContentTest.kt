package com.japl.repzi.feature.widget

import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.testing.unit.hasText
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeWidgetContentTest {

    private val texts = WidgetTexts(line = "Push · 2 days ago", button = "Start")
    private val action: Action = actionStartActivity(Intent())

    @Test
    fun itDrawsTheLineAndTheButtonOfTheState() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(110.dp, 50.dp))

        provideComposable {
            HomeWidgetContent(texts, WidgetTarget.Start(2, "Pull"), openApp = action, startRoutine = { action })
        }

        onNode(hasText("Push · 2 days ago")).assertExists()
        onNode(hasText("Start")).assertExists()
    }

    @Test
    fun theButtonStartsTheRoutineItNames() = runGlanceAppWidgetUnitTest {
        val started = mutableListOf<Long>()
        setAppWidgetSize(DpSize(110.dp, 50.dp))

        provideComposable {
            HomeWidgetContent(texts, WidgetTarget.Resume(7, "Pull"), openApp = action, startRoutine = { started += it; action })
        }

        // The composition runs when the tree is first queried.
        onNode(hasText("Start")).assertExists()
        assertEquals(listOf(7L), started)
    }

    @Test
    fun withoutRoutinesTheButtonOnlyOpensTheApp() = runGlanceAppWidgetUnitTest {
        val started = mutableListOf<Long>()
        setAppWidgetSize(DpSize(110.dp, 50.dp))

        provideComposable {
            HomeWidgetContent(texts.copy(button = "Create a routine"), WidgetTarget.NoRoutines, openApp = action, startRoutine = { started += it; action })
        }

        onNode(hasText("Create a routine")).assertExists()
        assertEquals(emptyList(), started)
    }
}
