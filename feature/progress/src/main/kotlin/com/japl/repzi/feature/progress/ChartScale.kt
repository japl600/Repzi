package com.japl.repzi.feature.progress

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/** The vertical axis of the chart: [min] and [max] are round multiples of [step], which is the gap between ticks. */
internal data class ChartScale(val min: Double, val max: Double, val step: Double) {
    val ticks: List<Double>
        get() = generateSequence(min) { it + step }.takeWhile { it <= max + step / 2 }.toList()
}

private val NICE_MULTIPLES = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
private val WHOLE_MULTIPLES = listOf(1.0, 2.0, 5.0, 10.0)
private const val TARGET_INTERVALS = 4

/**
 * Picks an axis that contains [low]..[high] with about four round-numbered intervals. With [wholeNumbers] (reps) the
 * step is never a fraction, so the axis does not show half a rep.
 */
internal fun chartScale(low: Double, high: Double, wholeNumbers: Boolean = false): ChartScale {
    // A flat series has no range, so one is made up around it to keep the line away from the edges.
    val range = (high - low).takeIf { it > 0 } ?: max(high * 0.2, 5.0)
    val rawStep = range / TARGET_INTERVALS
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val multiples = if (wholeNumbers) WHOLE_MULTIPLES else NICE_MULTIPLES
    val step = multiples.map { it * magnitude }.first { it >= rawStep }.let { if (wholeNumbers) max(it, 1.0) else it }

    val min = floor(low / step) * step
    var max = ceil(high / step) * step
    if (max <= min) max = min + step
    return ChartScale(min, max, step)
}
