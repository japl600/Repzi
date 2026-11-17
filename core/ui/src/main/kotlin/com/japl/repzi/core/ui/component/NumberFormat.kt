package com.japl.repzi.core.ui.component

/** Shows whole numbers without the decimal part: 60 instead of 60.0. */
fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
