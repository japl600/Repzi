package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject

class DeleteRoutineUseCase @Inject constructor(
    private val repository: RoutineRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteRoutine(id)
}
