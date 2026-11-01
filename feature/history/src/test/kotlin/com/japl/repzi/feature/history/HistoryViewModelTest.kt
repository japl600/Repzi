package com.japl.repzi.feature.history

import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.DeleteSessionUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun session(id: Long, startedAtMillis: Long) = WorkoutSession(
        id = id,
        routineName = "Routine $id",
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        finishedAt = Instant.fromEpochMilliseconds(startedAtMillis + 60_000),
        exercises = emptyList(),
    )

    private val repository = FakeSessionRepository(listOf(session(1, 1_000), session(2, 9_000)))

    private fun createViewModel() = HistoryViewModel(
        observeSessions = ObserveSessionsUseCase(repository),
        deleteSession = DeleteSessionUseCase(repository),
    )

    @Test
    fun startsLoading() {
        assertTrue(createViewModel().uiState.value.isLoading)
    }

    @Test
    fun showsTheMostRecentSessionFirst() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 1L), state.sessions.map { it.id })
    }

    @Test
    fun deletionNeedsConfirmation() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        val target = viewModel.uiState.value.sessions.first()

        viewModel.onDeleteRequested(target)
        assertEquals(target, viewModel.uiState.value.sessionToDelete)
        assertEquals(2, repository.saved.size)

        viewModel.onDeleteConfirmed()
        assertNull(viewModel.uiState.value.sessionToDelete)
        assertEquals(listOf(1L), viewModel.uiState.value.sessions.map { it.id })
    }

    @Test
    fun dismissingTheDialogKeepsTheSession() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onDeleteRequested(viewModel.uiState.value.sessions.first())
        viewModel.onDeleteDismissed()

        assertNull(viewModel.uiState.value.sessionToDelete)
        assertEquals(2, viewModel.uiState.value.sessions.size)
    }
}
