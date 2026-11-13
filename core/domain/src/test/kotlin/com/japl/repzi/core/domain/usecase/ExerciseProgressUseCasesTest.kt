package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.repository.SessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ExerciseProgressUseCasesTest {

    private class StaticRepository(private val sessions: List<WorkoutSession>) : SessionRepository {
        override fun observeSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override suspend fun saveSession(session: WorkoutSession): Long = 0
        override suspend fun deleteSession(id: Long) = Unit
    }

    private val bench = Exercise(1, "Press de banca", MuscleGroup.CHEST, isCustom = false)
    private val pullUp = Exercise(2, "Dominadas", MuscleGroup.BACK, isCustom = false)
    private val squat = Exercise(3, "Sentadilla", MuscleGroup.LEGS, isCustom = false)

    private fun session(startedAtMillis: Long, vararg exercises: LoggedExercise) = WorkoutSession(
        id = startedAtMillis,
        routineName = "Routine",
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        finishedAt = Instant.fromEpochMilliseconds(startedAtMillis + 1_000),
        exercises = exercises.toList(),
    )

    private fun progress(exerciseId: Long, vararg sessions: WorkoutSession) =
        ObserveExerciseProgressUseCase(StaticRepository(sessions.toList()))(exerciseId)

    @Test
    fun givesTheHeaviestSetOfEachSessionOldestFirst() = runTest {
        val points = progress(
            bench.id,
            session(9_000, LoggedExercise(bench, listOf(LoggedSet(5, 80.0)))),
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(8, 60.0), LoggedSet(5, 65.0), LoggedSet(6, 62.5)))),
        ).first()

        assertEquals(
            listOf(
                ProgressPoint(Instant.fromEpochMilliseconds(1_000), 65.0, 5),
                ProgressPoint(Instant.fromEpochMilliseconds(9_000), 80.0, 5),
            ),
            points,
        )
    }

    @Test
    fun withTheSameWeightTheSetWithMoreRepsWins() = runTest {
        val points = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(3, 80.0), LoggedSet(6, 80.0), LoggedSet(4, 80.0)))),
        ).first()

        assertEquals(6, points.single().reps)
    }

    @Test
    fun ignoresOtherExercisesAndSessionsWithoutThisOne() = runTest {
        val points = progress(
            bench.id,
            session(1_000, LoggedExercise(squat, listOf(LoggedSet(5, 100.0)))),
            session(2_000, LoggedExercise(squat, listOf(LoggedSet(5, 110.0))), LoggedExercise(bench, listOf(LoggedSet(5, 70.0)))),
        ).first()

        assertEquals(listOf(70.0), points.map { it.maxWeightKg })
    }

    @Test
    fun poolsTheSetsWhenTheExerciseAppearsTwiceInASession() = runTest {
        val points = progress(
            bench.id,
            session(
                1_000,
                LoggedExercise(bench, listOf(LoggedSet(5, 70.0))),
                LoggedExercise(bench, listOf(LoggedSet(3, 75.0))),
            ),
        ).first()

        assertEquals(75.0, points.single().maxWeightKg)
    }

    @Test
    fun bodyweightSetsAreNotPlotted() = runTest {
        val points = progress(pullUp.id, session(1_000, LoggedExercise(pullUp, listOf(LoggedSet(10, 0.0))))).first()

        assertTrue(points.isEmpty())
    }

    @Test
    fun trackedExercisesAreTheOnesWithWeightedSetsWithoutRepeatsByName() = runTest {
        val repository = StaticRepository(
            listOf(
                session(1_000, LoggedExercise(squat, listOf(LoggedSet(5, 100.0))), LoggedExercise(pullUp, listOf(LoggedSet(10, 0.0)))),
                session(2_000, LoggedExercise(squat, listOf(LoggedSet(5, 105.0))), LoggedExercise(bench, listOf(LoggedSet(5, 70.0)))),
            ),
        )

        val tracked = ObserveTrackedExercisesUseCase(repository)().first()

        assertEquals(listOf(bench, squat), tracked)
    }
}
