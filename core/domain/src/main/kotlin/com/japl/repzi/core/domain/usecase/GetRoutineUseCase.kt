package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject

class GetRoutineUseCase @Inject constructor(
    private val repository: RoutineRepository,
) {
    suspend operator fun invoke(id: Long): Routine? = repository.getRoutine(id)
}
