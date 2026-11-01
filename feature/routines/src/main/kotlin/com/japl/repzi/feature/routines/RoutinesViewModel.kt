package com.japl.repzi.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.usecase.DeleteRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    observeRoutines: ObserveRoutinesUseCase,
    private val deleteRoutine: DeleteRoutineUseCase,
) : ViewModel() {

    private val routineToDelete = MutableStateFlow<Routine?>(null)

    val uiState: StateFlow<RoutinesUiState> = combine(observeRoutines(), routineToDelete) { routines, pending ->
        RoutinesUiState(isLoading = false, routines = routines, routineToDelete = pending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoutinesUiState(),
    )

    fun onDeleteRequested(routine: Routine) {
        routineToDelete.value = routine
    }

    fun onDeleteDismissed() {
        routineToDelete.value = null
    }

    fun onDeleteConfirmed() {
        val routine = routineToDelete.value ?: return
        routineToDelete.value = null
        viewModelScope.launch { deleteRoutine(routine.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
