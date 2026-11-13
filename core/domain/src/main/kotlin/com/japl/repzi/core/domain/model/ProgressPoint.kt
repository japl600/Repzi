package com.japl.repzi.core.domain.model

import kotlin.time.Instant

/**
 * What one exercise looked like in one session, so a list of them is its progress over time. Every figure is zero
 * when it does not apply: a bodyweight-only exercise has no weight, volume or one-rep max, only reps.
 *
 * @property maxWeightKg the heaviest weight used.
 * @property reps the reps done with that heaviest weight (the best of them when several sets share the weight).
 * @property volumeKg weight times reps, summed over all the sets.
 * @property estimatedOneRepMaxKg the best estimated one-rep max of the weighted sets, see [estimatedOneRepMax].
 * @property maxReps the most reps in a single set, with or without weight.
 */
data class ProgressPoint(
    val date: Instant,
    val maxWeightKg: Double,
    val reps: Int,
    val volumeKg: Double,
    val estimatedOneRepMaxKg: Double,
    val maxReps: Int,
)

/**
 * Epley estimate of the heaviest weight that could be lifted once: `weight * (1 + reps / 30)`. A real single is
 * its own one-rep max. The estimate gets unreliable beyond about ten reps.
 */
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double =
    if (reps <= 1) weightKg else weightKg * (1 + reps / 30.0)
