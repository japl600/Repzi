package com.japl.repzi.feature.settings

import com.japl.repzi.core.domain.usecase.ObserveRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.SetRestVibrationUseCase
import com.japl.repzi.core.testing.FakeSettingsRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
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
    )

    @Test
    fun uiStateStartsOn() {
        assertTrue(createViewModel().uiState.value.restVibration)
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
