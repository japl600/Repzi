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

    private val texts = WidgetTexts(
        title = "Last workout",
        subtitle = "Push · 2 days ago",
        summary = "52 min · 4200 kg",
        button = "Start Pull",
    )
    private val action: Action = actionStartActivity(Intent())

    @Test
    fun itDrawsTheTextsAndTheButtonOfTheState() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(110.dp, 110.dp))

        provideComposable {
            HomeWidgetContent(texts, WidgetTarget.Start(2, "Pull"), openApp = action, startRoutine = { action })
        }

        onNode(hasText("Last workout")).assertExists()
        onNode(hasText("Push · 2 days ago")).assertExists()
        onNode(hasText("52 min · 4200 kg")).assertExists()
        onNode(hasText("Start Pull")).assertExists()
    }

    @Test
    fun theButtonStartsTheRoutineItNames() = runGlanceAppWidgetUnitTest {
        val started = mutableListOf<Long>()
        setAppWidgetSize(DpSize(110.dp, 110.dp))

        provideComposable {
            HomeWidgetContent(texts, WidgetTarget.Resume(7, "Pull"), openApp = action, startRoutine = { started += it; action })
        }

        // The composition runs when the tree is first queried.
        onNode(hasText("Start Pull")).assertExists()
        assertEquals(listOf(7L), started)
    }

    @Test
    fun withoutRoutinesTheButtonOnlyOpensTheApp() = runGlanceAppWidgetUnitTest {
        val started = mutableListOf<Long>()
        setAppWidgetSize(DpSize(110.dp, 110.dp))

        provideComposable {
            HomeWidgetContent(texts.copy(button = "Create a routine"), WidgetTarget.NoRoutines, openApp = action, startRoutine = { started += it; action })
        }

        onNode(hasText("Create a routine")).assertExists()
        assertEquals(emptyList(), started)
    }

    @Test
    fun linesWithoutTextAreNotDrawn() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(110.dp, 110.dp))

        provideComposable {
            HomeWidgetContent(
                WidgetTexts("No workouts yet", null, null, "Start Pull"),
                WidgetTarget.Start(2, "Pull"),
                openApp = action,
                startRoutine = { action },
            )
        }

        onNode(hasText("No workouts yet")).assertExists()
        onNode(hasText("Push · 2 days ago")).assertDoesNotExist()
    }
}
