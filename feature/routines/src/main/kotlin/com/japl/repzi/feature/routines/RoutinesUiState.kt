package com.japl.repzi.feature.routines

import com.japl.repzi.core.domain.model.Routine

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val routines: List<Routine> = emptyList(),
    /** Routine waiting for the user to confirm its deletion. */
    val routineToDelete: Routine? = null,
)
