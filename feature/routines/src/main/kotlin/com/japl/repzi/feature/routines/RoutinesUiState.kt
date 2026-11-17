package com.japl.repzi.feature.routines

import com.japl.repzi.core.domain.model.Routine

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val routines: List<Routine> = emptyList(),
    /** Routine waiting for the user to confirm its deletion. */
    val routineToDelete: Routine? = null,
)

/** One-shot results of adding a predefined routine, shown once and not kept in the state. */
sealed interface RoutinesEvent {
    data class PredefinedAdded(val id: Long, val name: String) : RoutinesEvent
    data object PredefinedUnavailable : RoutinesEvent
}
