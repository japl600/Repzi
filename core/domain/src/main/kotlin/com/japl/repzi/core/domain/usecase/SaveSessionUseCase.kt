package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject

sealed interface SaveSessionResult {
    data class Saved(val id: Long) : SaveSessionResult
    data object NothingLogged : SaveSessionResult
}

/** Drops sets that were never filled in and refuses to save a session where nothing was actually performed. */
class SaveSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(session: WorkoutSession): SaveSessionResult {
        val performed = session.exercises
            .map { logged -> logged.copy(sets = logged.sets.filter { it.reps > 0 && it.weightKg >= 0 }) }
            .filter { it.sets.isNotEmpty() }
        if (performed.isEmpty()) return SaveSessionResult.NothingLogged
        return SaveSessionResult.Saved(repository.saveSession(session.copy(exercises = performed)))
    }
}
