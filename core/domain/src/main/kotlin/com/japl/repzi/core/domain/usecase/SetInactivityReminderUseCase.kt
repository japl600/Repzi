package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetInactivityReminderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setInactivityReminder(enabled)
}
