package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Routines sorted by name, ignoring case. */
class ObserveRoutinesUseCase @Inject constructor(
    private val repository: RoutineRepository,
) {
    operator fun invoke(): Flow<List<Routine>> =
        repository.observeRoutines().map { routines -> routines.sortedBy { it.name.lowercase() } }
}
