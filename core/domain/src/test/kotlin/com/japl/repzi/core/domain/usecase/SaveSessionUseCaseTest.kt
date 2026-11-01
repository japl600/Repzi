package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class SaveSessionUseCaseTest {

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private class RecordingRepository : SessionRepository {
        var saved: WorkoutSession? = null

        override fun observeSessions(): Flow<List<WorkoutSession>> = emptyFlow()
        override suspend fun saveSession(session: WorkoutSession): Long {
            saved = session
            return 9
        }
        override suspend fun deleteSession(id: Long) = Unit
    }

    private val repository = RecordingRepository()
    private val useCase = SaveSessionUseCase(repository)

    private fun session(vararg exercises: LoggedExercise) = WorkoutSession(
        routineName = "Push",
        startedAt = Instant.fromEpochMilliseconds(1_000),
        finishedAt = Instant.fromEpochMilliseconds(5_000),
        exercises = exercises.toList(),
    )

    @Test
    fun rejectsASessionWithoutExercises() = runTest {
        val result = useCase(session())

        assertEquals(SaveSessionResult.NothingLogged, result)
        assertNull(repository.saved)
    }

    @Test
    fun rejectsASessionWhereEverySetIsEmpty() = runTest {
        val result = useCase(session(LoggedExercise(squat, listOf(LoggedSet(reps = 0, weightKg = 60.0)))))

        assertEquals(SaveSessionResult.NothingLogged, result)
        assertNull(repository.saved)
    }

    @Test
    fun keepsOnlyThePerformedSetsAndExercises() = runTest {
        val result = useCase(
            session(
                LoggedExercise(squat, listOf(LoggedSet(5, 100.0), LoggedSet(0, 100.0))),
                LoggedExercise(bench, listOf(LoggedSet(0, 0.0))),
            ),
        )

        assertEquals(SaveSessionResult.Saved(9), result)
        assertEquals(listOf(LoggedExercise(squat, listOf(LoggedSet(5, 100.0)))), repository.saved?.exercises)
    }

    @Test
    fun acceptsBodyweightSetsWithZeroWeight() = runTest {
        val result = useCase(session(LoggedExercise(squat, listOf(LoggedSet(reps = 12, weightKg = 0.0)))))

        assertEquals(SaveSessionResult.Saved(9), result)
    }
}
