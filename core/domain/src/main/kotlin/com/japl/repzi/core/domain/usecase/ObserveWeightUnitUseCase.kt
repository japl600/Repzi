package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The unit weights are typed and shown in. */
class ObserveWeightUnitUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<WeightUnit> = repository.weightUnit
}
