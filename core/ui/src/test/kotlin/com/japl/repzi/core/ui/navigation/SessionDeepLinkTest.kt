package com.japl.repzi.core.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionDeepLinkTest {

    @Test
    fun theLinkOfARoutineHasItsIdInThePlaceOfThePlaceholder() {
        assertEquals("repzi://routine/42/session", sessionDeepLink(42))
    }

    @Test
    fun thePatternHasOneRoutineIdPlaceholder() {
        assertEquals(1, Regex("""\{routineId}""").findAll(SESSION_DEEP_LINK_PATTERN).count())
    }
}
