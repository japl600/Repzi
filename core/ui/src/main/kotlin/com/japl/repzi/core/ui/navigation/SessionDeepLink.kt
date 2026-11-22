package com.japl.repzi.core.ui.navigation

/**
 * The link that opens the workout of a routine. It lives here so the screen that answers to it (the session
 * destination) and whoever sends it (the home-screen widget) share one definition without depending on each other.
 * The placeholder name is the session destination's argument name.
 */
const val SESSION_DEEP_LINK_PATTERN = "repzi://routine/{routineId}/session"

fun sessionDeepLink(routineId: Long): String = SESSION_DEEP_LINK_PATTERN.replace("{routineId}", routineId.toString())
