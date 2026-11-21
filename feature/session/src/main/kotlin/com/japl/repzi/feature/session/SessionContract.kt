package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.WeightUnit

/** Everything the screen needs to render. A single immutable object: the screen is a pure function of it. */
data class SessionState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    val exercises: List<ExerciseLog> = emptyList(),
    val error: SessionError? = null,
    val showDiscardDialog: Boolean = false,
    val isSaving: Boolean = false,
    /** The rest in progress, or null while the user is not resting. */
    val rest: RestCountdown? = null,
    /** Index of the exercise whose rest timer is being edited, or null while the picker is closed. */
    val restEditing: Int? = null,
    /** The unit the weight fields are typed in; read again at the moment the workout is saved. */
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    /** True once the user typed anything, which is when leaving the screen needs a confirmation. */
    val hasInput: Boolean
        get() = exercises.any { log -> log.sets.any { it.reps.isNotEmpty() || it.weight.isNotEmpty() } }
}

data class ExerciseLog(
    val exercise: Exercise,
    val sets: List<SetInput>,
    /** Seconds to rest after each set of this exercise, or null when the routine has no rest timer for it. */
    val restSeconds: Int? = null,
)

/**
 * Raw text of a set: the text fields own what the user typed and parsing happens only when saving. [done] is the
 * tick that starts the rest timer; it does not decide whether the set is saved.
 */
data class SetInput(
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false,
)

/** A rest that is running: [totalSeconds] is its length and [remainingSeconds] what is left, rounded up. */
data class RestCountdown(
    val totalSeconds: Int,
    val remainingSeconds: Int,
) {
    /** Share of the rest already taken, from 0 to 1, for the progress bar. */
    val progress: Float
        get() = if (totalSeconds <= 0) 1f else 1f - remainingSeconds.toFloat() / totalSeconds
}

/** How much the adjust buttons move the rest: fine steps when it is about to end, coarse ones otherwise. */
internal fun restAdjustStep(remainingSeconds: Int): Int =
    if (remainingSeconds <= FINE_ADJUST_LIMIT_SECONDS) FINE_ADJUST_SECONDS else COARSE_ADJUST_SECONDS

private const val FINE_ADJUST_LIMIT_SECONDS = 30
private const val FINE_ADJUST_SECONDS = 5
private const val COARSE_ADJUST_SECONDS = 15

enum class SessionError { NothingLogged }

/** Every way the user can change the state. */
sealed interface SessionIntent {
    data class WeightChanged(val exerciseIndex: Int, val setIndex: Int, val value: String) : SessionIntent
    data class RepsChanged(val exerciseIndex: Int, val setIndex: Int, val value: String) : SessionIntent
    data class SetAdded(val exerciseIndex: Int) : SessionIntent
    data class SetRemoved(val exerciseIndex: Int, val setIndex: Int) : SessionIntent

    /** Ticks or unticks a set. Ticking one starts the rest timer of its exercise, if it has one. */
    data class SetDoneToggled(val exerciseIndex: Int, val setIndex: Int) : SessionIntent

    data class RestEditRequested(val exerciseIndex: Int) : SessionIntent
    data object RestEditDismissed : SessionIntent

    /** The user picked a new rest for the exercise; null turns its rest timer off. It is saved in the routine. */
    data class RestChanged(val exerciseIndex: Int, val seconds: Int?) : SessionIntent

    /** Moves the end of the rest in progress by [deltaSeconds], which may be negative. */
    data class RestAdjusted(val deltaSeconds: Int) : SessionIntent
    data object RestSkipped : SessionIntent

    data object FinishClicked : SessionIntent
    data object BackClicked : SessionIntent
    data object DiscardConfirmed : SessionIntent
    data object DiscardDismissed : SessionIntent
}

/** One-shot events that must not be replayed when the screen is recreated. */
sealed interface SessionEffect {
    data object Close : SessionEffect

    /** The rest ran out: the screen buzzes so the user notices without looking at the phone. */
    data object RestFinished : SessionEffect
}
