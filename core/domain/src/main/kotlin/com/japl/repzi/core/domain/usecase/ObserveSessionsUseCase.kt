package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Finished workouts, most recent first. */
class ObserveSessionsUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    operator fun invoke(): Flow<List<WorkoutSession>> =
        repository.observeSessions().map { sessions -> sessions.sortedByDescending { it.startedAt } }
}
