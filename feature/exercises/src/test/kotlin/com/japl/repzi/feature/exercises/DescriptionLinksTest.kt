package com.japl.repzi.feature.exercises

import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptionLinksTest {

    private val opened = mutableListOf<String>()
    private val listener = webLinkListener { opened += it }

    private fun click(url: String) = listener.onClick(LinkAnnotation.Url(url))

    @Test
    fun opensHttpsAndHttpLinks() {
        click("https://example.org/a")
        click("HTTP://example.org/b")

        assertEquals(listOf("https://example.org/a", "HTTP://example.org/b"), opened)
    }

    @Test
    fun ignoresEveryOtherScheme() {
        // The description is untrusted text from contributors: it must not launch the dialer, the store or another app.
        click("tel:123456")
        click("market://details?id=com.example")
        click("intent://scan/#Intent;scheme=zxing;end")
        click("file:///sdcard/x")
        click("javascript:alert(1)")
        click("not a url")

        assertEquals(emptyList(), opened)
    }
}
