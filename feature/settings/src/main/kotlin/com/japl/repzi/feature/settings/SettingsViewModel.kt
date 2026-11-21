package com.japl.repzi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.usecase.ObserveInactivityDaysUseCase
import com.japl.repzi.core.domain.usecase.ObserveInactivityReminderUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.ObserveWeightUnitUseCase
import com.japl.repzi.core.domain.usecase.SetInactivityDaysUseCase
import com.japl.repzi.core.domain.usecase.SetInactivityReminderUseCase
import com.japl.repzi.core.domain.usecase.SetRestNotificationUseCase
import com.japl.repzi.core.domain.usecase.SetRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.SetWeightUnitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeRestVibration: ObserveRestVibrationUseCase,
    private val setRestVibration: SetRestVibrationUseCase,
    observeRestNotification: ObserveRestNotificationUseCase,
    private val setRestNotification: SetRestNotificationUseCase,
    observeInactivityReminder: ObserveInactivityReminderUseCase,
    private val setInactivityReminder: SetInactivityReminderUseCase,
    observeInactivityDays: ObserveInactivityDaysUseCase,
    private val setInactivityDays: SetInactivityDaysUseCase,
    observeWeightUnit: ObserveWeightUnitUseCase,
    private val setWeightUnit: SetWeightUnitUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        observeRestVibration(),
        observeRestNotification(),
        observeInactivityReminder(),
        observeInactivityDays(),
        observeWeightUnit(),
    ) { vibration, notification, reminder, days, unit ->
        SettingsUiState(
            restVibration = vibration,
            restNotification = notification,
            inactivityReminder = reminder,
            inactivityDays = days,
            weightUnit = unit,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun onRestVibrationChange(enabled: Boolean) {
        viewModelScope.launch { setRestVibration(enabled) }
    }

    fun onRestNotificationChange(enabled: Boolean) {
        viewModelScope.launch { setRestNotification(enabled) }
    }

    fun onInactivityReminderChange(enabled: Boolean) {
        viewModelScope.launch { setInactivityReminder(enabled) }
    }

    fun onWeightUnitChange(unit: WeightUnit) {
        viewModelScope.launch { setWeightUnit(unit) }
    }

    fun onInactivityDaysChange(days: Int) {
        viewModelScope.launch { setInactivityDays(days) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
