package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject

sealed interface SaveRoutineResult {
    data class Saved(val id: Long) : SaveRoutineResult
    data object BlankName : SaveRoutineResult
    data object NoExercises : SaveRoutineResult
}

/** Validates the routine before persisting it, so the rules live in the domain and not in a ViewModel. */
class SaveRoutineUseCase @Inject constructor(
    private val repository: RoutineRepository,
) {
    suspend operator fun invoke(routine: Routine): SaveRoutineResult {
        val name = routine.name.trim()
        return when {
            name.isEmpty() -> SaveRoutineResult.BlankName
            routine.exercises.isEmpty() -> SaveRoutineResult.NoExercises
            else -> SaveRoutineResult.Saved(repository.saveRoutine(routine.copy(name = name)))
        }
    }
}
