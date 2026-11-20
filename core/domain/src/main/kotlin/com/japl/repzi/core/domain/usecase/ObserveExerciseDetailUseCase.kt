package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.domain.repository.ExerciseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The detail of one exercise, or null when it does not exist. */
class ObserveExerciseDetailUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    operator fun invoke(id: Long): Flow<ExerciseDetail?> = repository.observeDetail(id)
}
