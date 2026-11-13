package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.ProgressPoint
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.model.estimatedOneRepMax
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
    fun givesOnePointPerSessionOldestFirst() = runTest {
        val points = progress(
            bench.id,
            session(9_000, LoggedExercise(bench, listOf(LoggedSet(5, 80.0)))),
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(8, 60.0), LoggedSet(5, 65.0), LoggedSet(6, 62.5)))),
        ).first()

        assertEquals(listOf(1_000L, 9_000L), points.map { it.date.toEpochMilliseconds() })
    }

    @Test
    fun theHeaviestSetGivesTheMaxWeightAndItsReps() = runTest {
        val point = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(8, 60.0), LoggedSet(5, 65.0), LoggedSet(6, 62.5)))),
        ).first().single()

        assertEquals(65.0, point.maxWeightKg)
        assertEquals(5, point.reps)
    }

    @Test
    fun withTheSameWeightTheSetWithMoreRepsWins() = runTest {
        val point = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(3, 80.0), LoggedSet(6, 80.0), LoggedSet(4, 80.0)))),
        ).first().single()

        assertEquals(6, point.reps)
    }

    @Test
    fun volumeAddsWeightTimesRepsOfEverySet() = runTest {
        val point = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(10, 60.0), LoggedSet(8, 62.5), LoggedSet(5, 65.0)))),
        ).first().single()

        // 600 + 500 + 325
        assertEquals(1_425.0, point.volumeKg)
    }

    @Test
    fun theOneRepMaxIsTheBestEstimateNotTheHeaviestSet() = runTest {
        val point = progress(
            bench.id,
            // 100 x 3 is heavier, but 80 x 12 estimates a higher max (80 * 1.4 = 112 against 110).
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(3, 100.0), LoggedSet(12, 80.0)))),
        ).first().single()

        assertEquals(100.0, point.maxWeightKg)
        assertEquals(112.0, point.estimatedOneRepMaxKg, absoluteTolerance = 1e-9)
    }

    @Test
    fun aSingleIsItsOwnOneRepMax() {
        assertEquals(140.0, estimatedOneRepMax(140.0, 1))
        assertEquals(100.0 * (1 + 5 / 30.0), estimatedOneRepMax(100.0, 5), absoluteTolerance = 1e-9)
    }

    @Test
    fun maxRepsIsTheBestSingleSet() = runTest {
        val point = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(8, 60.0), LoggedSet(12, 40.0), LoggedSet(6, 65.0)))),
        ).first().single()

        assertEquals(12, point.maxReps)
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
        val point = progress(
            bench.id,
            session(
                1_000,
                LoggedExercise(bench, listOf(LoggedSet(5, 70.0))),
                LoggedExercise(bench, listOf(LoggedSet(3, 75.0))),
            ),
        ).first().single()

        assertEquals(75.0, point.maxWeightKg)
        assertEquals(575.0, point.volumeKg) // 350 + 225
    }

    @Test
    fun bodyweightSetsGiveRepsButNoWeightFigures() = runTest {
        val point = progress(
            pullUp.id,
            session(1_000, LoggedExercise(pullUp, listOf(LoggedSet(10, 0.0), LoggedSet(8, 0.0)))),
        ).first().single()

        assertEquals(ProgressPoint(Instant.fromEpochMilliseconds(1_000), 0.0, 10, 0.0, 0.0, 10), point)
    }

    @Test
    fun addedWeightOnABodyweightExerciseIsTreatedAsWeight() = runTest {
        val point = progress(
            pullUp.id,
            session(1_000, LoggedExercise(pullUp, listOf(LoggedSet(10, 0.0), LoggedSet(5, 10.0)))),
        ).first().single()

        assertEquals(10.0, point.maxWeightKg)
        assertEquals(5, point.reps)
        assertEquals(50.0, point.volumeKg)
        assertEquals(10, point.maxReps)
    }

    @Test
    fun setsWithoutRepsAreIgnored() = runTest {
        val points = progress(
            bench.id,
            session(1_000, LoggedExercise(bench, listOf(LoggedSet(0, 100.0)))),
        ).first()

        assertTrue(points.isEmpty())
    }

    @Test
    fun trackedExercisesAreTheOnesWithLoggedSetsWithoutRepeatsByName() = runTest {
        val repository = StaticRepository(
            listOf(
                session(1_000, LoggedExercise(squat, listOf(LoggedSet(5, 100.0))), LoggedExercise(pullUp, listOf(LoggedSet(10, 0.0)))),
                session(2_000, LoggedExercise(squat, listOf(LoggedSet(5, 105.0))), LoggedExercise(bench, listOf(LoggedSet(5, 70.0)))),
            ),
        )

        val tracked = ObserveTrackedExercisesUseCase(repository)().first()

        assertEquals(listOf(pullUp, bench, squat).sortedBy { it.name }, tracked)
    }
}
