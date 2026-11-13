package com.japl.repzi.core.ui.component

import java.util.Locale

/** Formats a rest in seconds as minutes and seconds, for example 90 -> "1:30" and 10 -> "0:10". */
fun formatRestTime(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
}
