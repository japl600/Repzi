package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetInactivityDaysUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(days: Int) = repository.setInactivityDays(days)
}
