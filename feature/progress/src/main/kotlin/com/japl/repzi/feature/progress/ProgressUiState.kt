package com.japl.repzi.feature.progress

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.ProgressPoint
import kotlin.math.roundToInt

/** How far back the chart looks. [days] is null for the whole history. */
enum class ProgressRange(val days: Int?) {
    MONTH(30),
    QUARTER(90),
    HALF_YEAR(180),
    ALL(null),
}

data class ProgressUiState(
    val isLoading: Boolean = true,
    /** Exercises that have progress to show, by name. */
    val exercises: List<Exercise> = emptyList(),
    val selected: Exercise? = null,
    val range: ProgressRange = ProgressRange.ALL,
    /** The metric being plotted; always one of [availableMetrics] when there are any. */
    val metric: ProgressMetric = ProgressMetric.MAX_WEIGHT,
    /** Metrics the selected exercise has data for: a bodyweight exercise only has reps. */
    val availableMetrics: List<ProgressMetric> = emptyList(),
    /** Sessions inside [range], oldest first. */
    val points: List<ProgressPoint> = emptyList(),
) {
    /** The values of [metric] inside [range]; sessions where the metric does not apply are left out. */
    internal val series: List<ChartPoint> = points.mapNotNull { point ->
        metric.valueIn(point)?.let { ChartPoint(point.date, it) }
    }

    val record: Double? get() = series.maxOfOrNull { it.value }

    /** Gain from the first to the last point of the range, or null with fewer than two sessions. */
    val change: Double?
        get() = if (series.size >= 2) series.last().value - series.first().value else null

    val changePercent: Int?
        get() = change?.let { (it / series.first().value * 100).roundToInt() }
}
