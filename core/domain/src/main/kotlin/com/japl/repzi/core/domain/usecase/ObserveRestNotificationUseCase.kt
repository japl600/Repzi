package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether a notification tells the user a rest is over while the app is not on screen. */
class ObserveRestNotificationUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.restNotification
}
