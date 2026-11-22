package com.japl.repzi.feature.exercises

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderedListsTest {

    @Test
    fun numbersTheItemsOfAnOrderedList() {
        assertEquals(
            "<p>1. Stand up.</p><p>2. Sit down.</p>",
            numberOrderedLists("<ol><li>Stand up.</li><li>Sit down.</li></ol>"),
        )
    }

    @Test
    fun keepsTheTextAroundTheList() {
        assertEquals(
            "<p>Intro</p><p>1. A</p><p>Outro</p>",
            numberOrderedLists("<p>Intro</p><ol><li>A</li></ol><p>Outro</p>"),
        )
    }

    @Test
    fun eachListStartsAgainAtOne() {
        assertEquals(
            "<p>1. A</p><p>2. B</p><p>1. C</p>",
            numberOrderedLists("<ol><li>A</li><li>B</li></ol><ol><li>C</li></ol>"),
        )
    }

    @Test
    fun keepsTheMarkupInsideAnItem() {
        assertEquals(
            "<p>1. Hold the <b>bell</b> <a href=\"https://x\">here</a>.</p>",
            numberOrderedLists("<ol><li>Hold the <b>bell</b> <a href=\"https://x\">here</a>.</li></ol>"),
        )
    }

    @Test
    fun attributesCaseAndLineBreaksDoNotMatter() {
        assertEquals(
            "<p>1. A\nB</p><p>2. C</p>",
            numberOrderedLists("<OL class=\"steps\">\n<LI style=\"x\">A\nB</LI>\n<li>C</li>\n</OL>"),
        )
    }

    @Test
    fun leavesUnorderedListsAndPlainTextAlone() {
        val html = "<p>Text</p><ul><li>A</li><li>B</li></ul>"

        assertEquals(html, numberOrderedLists(html))
    }

    @Test
    fun anOrderedListWithoutItemsIsLeftAlone() {
        val html = "<ol>just text</ol>"

        assertEquals(html, numberOrderedLists(html))
    }
}
