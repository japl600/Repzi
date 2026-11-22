package com.japl.repzi.feature.exercises

private val ORDERED_LIST = Regex("<ol[^>]*>(.*?)</ol>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val LIST_ITEM = Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

/**
 * Turns every `<ol>` into numbered paragraphs, because the HTML-to-text conversion that follows keeps the items but
 * drops their numbers, and wger writes its steps as ordered lists. Each list counts from 1; what is inside an item
 * (emphasis, links) is kept as it is. A list with no `<li>` and everything that is not an `<ol>` is left untouched.
 */
internal fun numberOrderedLists(html: String): String = ORDERED_LIST.replace(html) { list ->
    val items = LIST_ITEM.findAll(list.groupValues[1]).map { it.groupValues[1] }.toList()
    if (items.isEmpty()) {
        list.value
    } else {
        items.mapIndexed { index, item -> "<p>${index + 1}. $item</p>" }.joinToString("")
    }
}
