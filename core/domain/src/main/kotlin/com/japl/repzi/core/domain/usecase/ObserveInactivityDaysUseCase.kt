package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Days without a workout before the reminder fires. */
class ObserveInactivityDaysUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Int> = repository.inactivityDays
}
