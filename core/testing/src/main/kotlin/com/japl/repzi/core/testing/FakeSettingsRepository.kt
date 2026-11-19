package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initialRestVibration: Boolean = true) : SettingsRepository {

    private val vibration = MutableStateFlow(initialRestVibration)

    override val restVibration: Flow<Boolean> = vibration

    override suspend fun setRestVibration(enabled: Boolean) {
        vibration.value = enabled
    }
}
