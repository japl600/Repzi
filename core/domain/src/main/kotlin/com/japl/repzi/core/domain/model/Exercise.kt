package com.japl.repzi.core.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup,
    val isCustom: Boolean,
)

enum class MuscleGroup {
    CHEST,
    BACK,
    LEGS,
    SHOULDERS,
    ARMS,
    CORE,
}
