package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether the user is reminded to train after some days without a workout. */
class ObserveInactivityReminderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.inactivityReminder
}
