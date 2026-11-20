package com.japl.repzi.feature.exercises

import com.japl.repzi.core.domain.model.ExerciseDetail

/** [detail] stays null once loaded when the exercise does not exist (for example, a stale link). */
data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    val detail: ExerciseDetail? = null,
)
