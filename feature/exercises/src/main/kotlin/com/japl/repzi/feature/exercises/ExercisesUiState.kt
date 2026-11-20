package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup

data class ExercisesUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val selectedGroup: MuscleGroup? = null,
    /** Ids of the exercises with logged weight, which are the ones that get a progress button. */
    val exercisesWithProgress: Set<Long> = emptySet(),
    /** True when the library holds exercises from the wger catalog, so the attribution line has to be shown. */
    val hasCatalogContent: Boolean = false,
)
