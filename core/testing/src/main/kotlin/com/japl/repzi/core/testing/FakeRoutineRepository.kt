package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRoutineRepository(initial: List<Routine> = emptyList()) : RoutineRepository {

    private val routines = MutableStateFlow(initial)

    override fun observeRoutines(): Flow<List<Routine>> = routines

    override suspend fun getRoutine(id: Long): Routine? = routines.value.firstOrNull { it.id == id }

    override suspend fun saveRoutine(routine: Routine): Long {
        val id = if (routine.id == Routine.NEW_ID) (routines.value.maxOfOrNull { it.id } ?: 0) + 1 else routine.id
        routines.value = routines.value.filterNot { it.id == id } + routine.copy(id = id)
        return id
    }

    override suspend fun deleteRoutine(id: Long) {
        routines.value = routines.value.filterNot { it.id == id }
    }
}
