package com.japl.repzi.feature.session

private const val MAX_REPS_DIGITS = 4
private const val MAX_WEIGHT_CHARS = 7

/**
 * Pure state transition for the intents that only change what is on screen. Intents with side effects (saving,
 * closing) are handled by the ViewModel.
 */
internal fun SessionState.reduce(intent: SessionIntent): SessionState = when (intent) {
    is SessionIntent.WeightChanged -> updateSet(intent.exerciseIndex, intent.setIndex) {
        it.copy(weight = intent.value.toWeightInput())
    }
    is SessionIntent.RepsChanged -> updateSet(intent.exerciseIndex, intent.setIndex) {
        it.copy(reps = intent.value.filter(Char::isDigit).take(MAX_REPS_DIGITS))
    }
    is SessionIntent.SetAdded -> updateSets(intent.exerciseIndex) { sets ->
        // The new set starts as a copy of the previous one: lifters usually repeat the same weight and reps.
        sets + (sets.lastOrNull() ?: SetInput())
    }
    is SessionIntent.SetRemoved -> updateSets(intent.exerciseIndex) { sets ->
        // The last remaining set is never removed, so every exercise always has a row to type in.
        if (sets.size > 1) sets.filterIndexed { index, _ -> index != intent.setIndex } else sets
    }
    SessionIntent.BackClicked -> copy(showDiscardDialog = hasInput)
    SessionIntent.DiscardDismissed -> copy(showDiscardDialog = false)
    SessionIntent.FinishClicked, SessionIntent.DiscardConfirmed -> this
}

private fun SessionState.updateSets(exerciseIndex: Int, change: (List<SetInput>) -> List<SetInput>): SessionState {
    if (exerciseIndex !in exercises.indices) return this
    val updated = exercises.mapIndexed { index, log ->
        if (index == exerciseIndex) log.copy(sets = change(log.sets)) else log
    }
    return copy(exercises = updated, error = null)
}

private fun SessionState.updateSet(exerciseIndex: Int, setIndex: Int, change: (SetInput) -> SetInput) =
    updateSets(exerciseIndex) { sets ->
        sets.mapIndexed { index, set -> if (index == setIndex) change(set) else set }
    }

/** Accepts digits and a single decimal separator (dot or comma, normalized to a dot). */
private fun String.toWeightInput(): String {
    val normalized = replace(',', '.').filter { it.isDigit() || it == '.' }
    val firstDot = normalized.indexOf('.')
    val singleDot = if (firstDot < 0) {
        normalized
    } else {
        normalized.substring(0, firstDot + 1) + normalized.substring(firstDot + 1).replace(".", "")
    }
    return singleDot.take(MAX_WEIGHT_CHARS)
}
