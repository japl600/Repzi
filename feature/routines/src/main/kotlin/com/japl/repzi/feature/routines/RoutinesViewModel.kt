package com.japl.repzi.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.japl.repzi.core.domain.model.PredefinedRoutine
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.usecase.AddPredefinedRoutineResult
import com.japl.repzi.core.domain.usecase.AddPredefinedRoutineUseCase
import com.japl.repzi.core.domain.usecase.DeleteRoutineUseCase
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    observeRoutines: ObserveRoutinesUseCase,
    private val deleteRoutine: DeleteRoutineUseCase,
    private val addPredefinedRoutine: AddPredefinedRoutineUseCase,
) : ViewModel() {

    private val routineToDelete = MutableStateFlow<Routine?>(null)

    val uiState: StateFlow<RoutinesUiState> = combine(observeRoutines(), routineToDelete) { routines, pending ->
        RoutinesUiState(isLoading = false, routines = routines, routineToDelete = pending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoutinesUiState(),
    )

    private val _events = Channel<RoutinesEvent>(Channel.BUFFERED)
    val events: Flow<RoutinesEvent> = _events.receiveAsFlow()

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

    /** [name] is the template's name in the language the app is showing, chosen by the screen. */
    fun onAddPredefined(template: PredefinedRoutine, name: String) {
        viewModelScope.launch {
            val event = when (val result = addPredefinedRoutine(template, name)) {
                is AddPredefinedRoutineResult.Added -> RoutinesEvent.PredefinedAdded(result.id, result.name)
                AddPredefinedRoutineResult.Unavailable -> RoutinesEvent.PredefinedUnavailable
            }
            _events.send(event)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
