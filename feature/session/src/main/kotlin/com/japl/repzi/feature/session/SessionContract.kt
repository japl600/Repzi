package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.Exercise

/** Everything the screen needs to render. A single immutable object: the screen is a pure function of it. */
data class SessionState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val exercises: List<ExerciseLog> = emptyList(),
    val error: SessionError? = null,
    val showDiscardDialog: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** True once the user typed anything, which is when leaving the screen needs a confirmation. */
    val hasInput: Boolean
        get() = exercises.any { log -> log.sets.any { it.reps.isNotEmpty() || it.weight.isNotEmpty() } }
}

data class ExerciseLog(
    val exercise: Exercise,
    val sets: List<SetInput>,
)

/** Raw text of a set: the text fields own what the user typed and parsing happens only when saving. */
data class SetInput(
    val weight: String = "",
    val reps: String = "",
)

enum class SessionError { NothingLogged }

/** Every way the user can change the state. */
sealed interface SessionIntent {
    data class WeightChanged(val exerciseIndex: Int, val setIndex: Int, val value: String) : SessionIntent
    data class RepsChanged(val exerciseIndex: Int, val setIndex: Int, val value: String) : SessionIntent
    data class SetAdded(val exerciseIndex: Int) : SessionIntent
    data class SetRemoved(val exerciseIndex: Int, val setIndex: Int) : SessionIntent
    data object FinishClicked : SessionIntent
    data object BackClicked : SessionIntent
    data object DiscardConfirmed : SessionIntent
    data object DiscardDismissed : SessionIntent
}

/** One-shot events that must not be replayed when the screen is recreated. */
sealed interface SessionEffect {
    data object Close : SessionEffect
}
