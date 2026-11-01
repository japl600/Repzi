package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class SaveRoutineUseCaseTest {

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private class RecordingRepository : RoutineRepository {
        var saved: Routine? = null

        override fun observeRoutines(): Flow<List<Routine>> = emptyFlow()
        override suspend fun getRoutine(id: Long): Routine? = null
        override suspend fun saveRoutine(routine: Routine): Long {
            saved = routine
            return 7
        }
        override suspend fun deleteRoutine(id: Long) = Unit
    }

    private val repository = RecordingRepository()
    private val useCase = SaveRoutineUseCase(repository)

    @Test
    fun rejectsABlankName() = runTest {
        val result = useCase(Routine(name = "   ", exercises = listOf(squat)))

        assertEquals(SaveRoutineResult.BlankName, result)
        assertNull(repository.saved)
    }

    @Test
    fun rejectsARoutineWithoutExercises() = runTest {
        val result = useCase(Routine(name = "Legs", exercises = emptyList()))

        assertEquals(SaveRoutineResult.NoExercises, result)
        assertNull(repository.saved)
    }

    @Test
    fun savesWithATrimmedNameAndReturnsTheId() = runTest {
        val result = useCase(Routine(name = "  Legs day ", exercises = listOf(squat)))

        assertEquals(SaveRoutineResult.Saved(7), result)
        assertEquals(Routine(name = "Legs day", exercises = listOf(squat)), repository.saved)
    }
}
