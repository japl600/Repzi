package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.repository.DEFAULT_INACTIVITY_DAYS
import com.japl.repzi.core.domain.repository.INACTIVITY_DAYS_RANGE
import com.japl.repzi.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initialRestVibration: Boolean = true,
    initialRestNotification: Boolean = false,
    initialInactivityReminder: Boolean = false,
    initialInactivityDays: Int = DEFAULT_INACTIVITY_DAYS,
    initialWeightUnit: WeightUnit = WeightUnit.KG,
) : SettingsRepository {

    private val vibration = MutableStateFlow(initialRestVibration)
    private val notification = MutableStateFlow(initialRestNotification)
    private val reminder = MutableStateFlow(initialInactivityReminder)
    private val days = MutableStateFlow(initialInactivityDays)
    private val unit = MutableStateFlow(initialWeightUnit)

    override val restVibration: Flow<Boolean> = vibration

    override val restNotification: Flow<Boolean> = notification

    override val inactivityReminder: Flow<Boolean> = reminder

    override val inactivityDays: Flow<Int> = days

    override val weightUnit: Flow<WeightUnit> = unit

    override suspend fun setRestVibration(enabled: Boolean) {
        vibration.value = enabled
    }

    override suspend fun setRestNotification(enabled: Boolean) {
        notification.value = enabled
    }

    override suspend fun setInactivityReminder(enabled: Boolean) {
        reminder.value = enabled
    }

    override suspend fun setInactivityDays(days: Int) {
        this.days.value = days.coerceIn(INACTIVITY_DAYS_RANGE)
    }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        this.unit.value = unit
    }
}
