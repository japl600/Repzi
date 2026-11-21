package com.japl.repzi.feature.progress

import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WeightUnit
import kotlin.time.Instant

/** What the chart plots for an exercise. Higher is always better, so the record is the maximum. */
enum class ProgressMetric(val isWeight: Boolean) {
    MAX_WEIGHT(isWeight = true),
    ESTIMATED_ONE_REP_MAX(isWeight = true),
    VOLUME(isWeight = true),
    REPS(isWeight = false),
}

/** The figure of this metric in [point], or null when it does not apply (a bodyweight session has no weight). */
internal fun ProgressMetric.valueIn(point: ProgressPoint): Double? = when (this) {
    ProgressMetric.MAX_WEIGHT -> point.maxWeightKg
    ProgressMetric.ESTIMATED_ONE_REP_MAX -> point.estimatedOneRepMaxKg
    ProgressMetric.VOLUME -> point.volumeKg
    ProgressMetric.REPS -> point.maxReps.toDouble()
}.takeIf { it > 0 }

/** One plotted value: [value] is in kg for the weight metrics and in reps for [ProgressMetric.REPS]. */
internal data class ChartPoint(val date: Instant, val value: Double)

/** The figure of this metric in [point] as the user sees it: weights are stored in kilograms and shown in [unit]. */
internal fun ProgressMetric.shownIn(unit: WeightUnit, point: ProgressPoint): Double {
    val value = valueIn(point) ?: 0.0
    return if (isWeight) unit.fromKg(value) else value
}
