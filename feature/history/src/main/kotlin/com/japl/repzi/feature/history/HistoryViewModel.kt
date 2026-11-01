package com.japl.repzi.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.DeleteSessionUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeSessions: ObserveSessionsUseCase,
    private val deleteSession: DeleteSessionUseCase,
) : ViewModel() {

    private val sessionToDelete = MutableStateFlow<WorkoutSession?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(observeSessions(), sessionToDelete) { sessions, pending ->
        HistoryUiState(isLoading = false, sessions = sessions, sessionToDelete = pending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HistoryUiState(),
    )

    fun onDeleteRequested(session: WorkoutSession) {
        sessionToDelete.value = session
    }

    fun onDeleteDismissed() {
        sessionToDelete.value = null
    }

    fun onDeleteConfirmed() {
        val session = sessionToDelete.value ?: return
        sessionToDelete.value = null
        viewModelScope.launch { deleteSession(session.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
