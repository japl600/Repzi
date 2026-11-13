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
    /** Points inside [range], oldest first. */
    val points: List<ProgressPoint> = emptyList(),
) {
    val record: Double? get() = points.maxOfOrNull { it.maxWeightKg }

    /** Weight gained from the first to the last point of the range, or null with fewer than two sessions. */
    val changeKg: Double?
        get() = if (points.size >= 2) points.last().maxWeightKg - points.first().maxWeightKg else null

    val changePercent: Int?
        get() = changeKg?.let { (it / points.first().maxWeightKg * 100).roundToInt() }
}
