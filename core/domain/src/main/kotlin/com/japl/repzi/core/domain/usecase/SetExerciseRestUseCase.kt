package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.RestTimer
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject

/** Changes how long the exercise at [position] of a routine rests between sets. */
class SetExerciseRestUseCase @Inject constructor(
    private val repository: RoutineRepository,
) {
    /** Returns false, and stores nothing, when [restSeconds] is not one of [RestTimer.options]. */
    suspend operator fun invoke(routineId: Long, position: Int, restSeconds: Int?): Boolean {
        if (restSeconds != null && restSeconds !in RestTimer.options) return false
        repository.setRestSeconds(routineId, position, restSeconds)
        return true
    }
}
