package com.japl.repzi.core.ui.component

import com.japl.repzi.core.domain.model.WeightUnit

/** Shows whole numbers without the decimal part: 60 instead of 60.0. */
fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/**
 * A weight stored in kilograms, converted to [unit] and shown without the noise of the conversion: it is rounded to
 * two decimals, so 61.2349 kg is 135 lb and not 134.99999 lb.
 */
fun formatWeight(kg: Double, unit: WeightUnit): String = formatRounded(unit.fromKg(kg))

/** [value] rounded to two decimals and shown like [formatNumber]: the conversion between units leaves float noise. */
fun formatRounded(value: Double): String = formatNumber(Math.round(value * 100) / 100.0)
