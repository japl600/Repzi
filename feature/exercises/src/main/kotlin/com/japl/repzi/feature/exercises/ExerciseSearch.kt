package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")

/**
 * Whether the exercise name matches what the user typed. The match ignores case and accents
 * ("elevacion" finds "Elevación lateral") and every word of the query has to appear in the name,
 * in any order. A blank query matches everything.
 */
internal fun Exercise.matchesSearch(query: String): Boolean {
    val words = query.searchKey().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val name = name.searchKey()
    return words.all { it in name }
}

private fun String.searchKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .trim()
