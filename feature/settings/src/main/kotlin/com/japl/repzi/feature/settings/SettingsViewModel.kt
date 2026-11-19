package com.japl.repzi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.usecase.ObserveRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.SetRestVibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeRestVibration: ObserveRestVibrationUseCase,
    private val setRestVibration: SetRestVibrationUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeRestVibration()
        .map { enabled -> SettingsUiState(restVibration = enabled) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun onRestVibrationChange(enabled: Boolean) {
        viewModelScope.launch { setRestVibration(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
