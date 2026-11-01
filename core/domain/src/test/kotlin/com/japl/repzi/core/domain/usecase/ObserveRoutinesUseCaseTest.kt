package com.japl.repzi.core.domain.usecase

import app.cash.turbine.test
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ObserveRoutinesUseCaseTest {

    private val repository = object : RoutineRepository {
        override fun observeRoutines(): Flow<List<Routine>> = flowOf(
            listOf(
                Routine(1, "push", emptyList()),
                Routine(2, "Legs", emptyList()),
                Routine(3, "Back", emptyList()),
            ),
        )
        override suspend fun getRoutine(id: Long): Routine? = null
        override suspend fun saveRoutine(routine: Routine): Long = routine.id
        override suspend fun deleteRoutine(id: Long) = Unit
    }

    @Test
    fun ordersByNameIgnoringCase() = runTest {
        ObserveRoutinesUseCase(repository)().test {
            assertEquals(listOf("Back", "Legs", "push"), awaitItem().map { it.name })
            awaitComplete()
        }
    }
}
