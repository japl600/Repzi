package com.japl.repzi.core.domain.model

/** A named, ordered list of exercises. An [id] of [NEW_ID] means the routine has not been saved yet. */
data class Routine(
    val id: Long = NEW_ID,
    val name: String,
    val exercises: List<Exercise>,
) {
    companion object {
        const val NEW_ID = 0L
    }
}
