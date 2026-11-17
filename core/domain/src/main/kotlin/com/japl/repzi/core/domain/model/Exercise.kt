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
)

enum class MuscleGroup {
    CHEST,
    BACK,
    LEGS,
    SHOULDERS,
    ARMS,
    CORE,
}
