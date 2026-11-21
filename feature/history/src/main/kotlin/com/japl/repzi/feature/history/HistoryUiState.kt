package com.japl.repzi.feature.history

import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.model.WorkoutSession

data class HistoryUiState(
    val isLoading: Boolean = true,
    /** Most recent first. */
    val sessions: List<WorkoutSession> = emptyList(),
    /** Session waiting for the user to confirm its deletion. */
    val sessionToDelete: WorkoutSession? = null,
    /** The unit the weights are shown in. */
    val weightUnit: WeightUnit = WeightUnit.KG,
)
