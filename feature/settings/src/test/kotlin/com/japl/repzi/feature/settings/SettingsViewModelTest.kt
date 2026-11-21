package com.japl.repzi.feature.settings

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
import com.japl.repzi.core.testing.FakeSettingsRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    private fun createViewModel() = SettingsViewModel(
        observeRestVibration = ObserveRestVibrationUseCase(repository),
        setRestVibration = SetRestVibrationUseCase(repository),
        observeRestNotification = ObserveRestNotificationUseCase(repository),
        setRestNotification = SetRestNotificationUseCase(repository),
        observeInactivityReminder = ObserveInactivityReminderUseCase(repository),
        setInactivityReminder = SetInactivityReminderUseCase(repository),
        observeInactivityDays = ObserveInactivityDaysUseCase(repository),
        setInactivityDays = SetInactivityDaysUseCase(repository),
        observeWeightUnit = ObserveWeightUnitUseCase(repository),
        setWeightUnit = SetWeightUnitUseCase(repository),
    )

    @Test
    fun uiStateStartsOn() {
        assertTrue(createViewModel().uiState.value.restVibration)
    }

    @Test
    fun newSettingsStartOff() {
        val state = createViewModel().uiState.value

        assertFalse(state.restNotification)
        assertFalse(state.inactivityReminder)
        assertEquals(3, state.inactivityDays)
    }

    @Test
    fun uiStateFollowsTheNewSettings() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        repository.setRestNotification(true)
        repository.setInactivityReminder(true)
        repository.setInactivityDays(9)

        assertEquals(SettingsUiState(restNotification = true, inactivityReminder = true, inactivityDays = 9), viewModel.uiState.value)
    }

    @Test
    fun theWeightUnitStartsInKilogramsAndFollowsTheRepository() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        assertEquals(WeightUnit.KG, viewModel.uiState.value.weightUnit)

        repository.setWeightUnit(WeightUnit.LB)

        assertEquals(WeightUnit.LB, viewModel.uiState.value.weightUnit)
    }

    @Test
    fun choosingAWeightUnitWritesItToTheRepository() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightUnitChange(WeightUnit.LB)
        advanceUntilIdle()

        assertEquals(WeightUnit.LB, repository.weightUnit.first())
    }

    @Test
    fun changingTheNotificationSwitchesWritesToTheRepository() = runTest {
        val viewModel = createViewModel()

        viewModel.onRestNotificationChange(true)
        viewModel.onInactivityReminderChange(true)
        advanceUntilIdle()

        assertTrue(repository.restNotification.first())
        assertTrue(repository.inactivityReminder.first())
    }

    @Test
    fun changingTheDaysWritesToTheRepository() = runTest {
        val viewModel = createViewModel()

        viewModel.onInactivityDaysChange(9)
        advanceUntilIdle()

        assertEquals(9, repository.inactivityDays.first())
    }

    @Test
    fun uiStateFollowsTheRepository() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        repository.setRestVibration(false)

        assertFalse(viewModel.uiState.value.restVibration)
    }

    @Test
    fun changingTheSwitchWritesToTheRepository() = runTest {
        val viewModel = createViewModel()

        viewModel.onRestVibrationChange(false)
        advanceUntilIdle()

        assertFalse(repository.restVibration.first())
    }
}
