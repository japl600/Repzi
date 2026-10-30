package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup

data class ExercisesUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val selectedGroup: MuscleGroup? = null,
)
