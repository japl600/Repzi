package com.japl.repzi.feature.widget

import android.content.Context
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.component.formatWeight
import java.time.ZoneId
import kotlin.time.Instant

/** The words on the widget, already in the user's language. Null lines are not drawn. */
data class WidgetTexts(
    val title: String,
    val subtitle: String?,
    val summary: String?,
    val button: String,
)

/** Turns the widget's state into its texts. [now] and [zone] decide "Today", "Yesterday" and "N days ago". */
fun widgetTexts(context: Context, state: HomeWidgetState, now: Instant, zone: ZoneId): WidgetTexts {
    val resources = context.resources
    val last = state.lastWorkout
    return WidgetTexts(
        title = resources.getString(if (last == null) R.string.widget_no_workouts else R.string.widget_last_workout),
        subtitle = last?.let { "${it.routineName} · ${relativeDay(context, daysAgo(it.finishedAt, now, zone))}" },
        summary = last?.let {
            val minutes = resources.getString(R.string.widget_minutes, it.duration.inWholeMinutes.toInt())
            // A workout with no weight (bodyweight only) has nothing to say about volume.
            if (it.volumeKg > 0) "$minutes · ${formatWeight(it.volumeKg, state.weightUnit)} ${state.weightUnit.label}" else minutes
        },
        button = when (val target = state.target) {
            is WidgetTarget.Start -> resources.getString(R.string.widget_start, target.routineName)
            is WidgetTarget.Resume -> resources.getString(R.string.widget_resume, target.routineName)
            WidgetTarget.NoRoutines -> resources.getString(R.string.widget_create_routine)
        },
    )
}

private fun relativeDay(context: Context, days: Int): String = when (days) {
    0 -> context.getString(R.string.widget_today)
    1 -> context.getString(R.string.widget_yesterday)
    else -> context.resources.getQuantityString(R.plurals.widget_days_ago, days, days)
}

private val WeightUnit.label: String get() = name.lowercase()
