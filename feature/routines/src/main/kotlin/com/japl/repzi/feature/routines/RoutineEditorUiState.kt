package com.japl.repzi.feature.routines

import com.japl.repzi.core.domain.model.Exercise

enum class RoutineEditorError { BlankName, NoExercises }

data class RoutineEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val exercises: List<Exercise> = emptyList(),
    val error: RoutineEditorError? = null,
    /** Set once the routine is persisted; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
)
