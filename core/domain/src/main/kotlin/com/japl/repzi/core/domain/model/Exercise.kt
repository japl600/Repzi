package com.japl.repzi.core.domain.model

/**
 * [seedKey] is the stable key of a built-in exercise, null for the ones the user created. Predefined routines refer
 * to exercises by it, because names change with the language and ids differ between installs.
 */
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup,
    val isCustom: Boolean,
    val seedKey: String? = null,
    /** True for exercises that come from the wger catalog (they have a detail screen with author and license). */
    val fromCatalog: Boolean = false,
)

enum class MuscleGroup {
    CHEST,
    BACK,
    LEGS,
    SHOULDERS,
    ARMS,
    CORE,
}
