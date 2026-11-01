package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SessionRepository
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteSession(id)
}
