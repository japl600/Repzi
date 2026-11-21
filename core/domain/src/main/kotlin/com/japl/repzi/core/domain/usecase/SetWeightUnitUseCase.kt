package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetWeightUnitUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(unit: WeightUnit) = repository.setWeightUnit(unit)
}
