package com.japl.repzi.feature.widget

import android.content.Context
import java.time.ZoneId
import kotlin.time.Instant

/** The words on the widget, already in the user's language: a gray line about the workouts and the button. */
data class WidgetTexts(
    val line: String,
    val button: String,
)

/** Turns the widget's state into its texts. [now] and [zone] decide "Today", "Yesterday" and "N days ago". */
fun widgetTexts(context: Context, state: HomeWidgetState, now: Instant, zone: ZoneId): WidgetTexts {
    val resources = context.resources
    val last = state.lastWorkout
    return WidgetTexts(
        // A workout in progress is what matters most, so it takes the line the last workout would have.
        line = when {
            state.target is WidgetTarget.Resume -> resources.getString(R.string.widget_in_progress, state.target.routineName)
            last != null -> "${last.routineName} · ${relativeDay(context, daysAgo(last.finishedAt, now, zone))}"
            else -> resources.getString(R.string.widget_no_workouts)
        },
        button = when (state.target) {
            // Just "Start", also when resuming: the gray line says if the workout is already in progress.
            is WidgetTarget.Start, is WidgetTarget.Resume -> resources.getString(R.string.widget_start)
            WidgetTarget.NoRoutines -> resources.getString(R.string.widget_create_routine)
        },
    )
}

private fun relativeDay(context: Context, days: Int): String = when (days) {
    0 -> context.getString(R.string.widget_today)
    1 -> context.getString(R.string.widget_yesterday)
    else -> context.resources.getQuantityString(R.plurals.widget_days_ago, days, days)
}
