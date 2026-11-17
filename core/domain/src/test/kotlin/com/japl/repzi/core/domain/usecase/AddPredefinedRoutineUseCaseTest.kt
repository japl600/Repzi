package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.PredefinedRoutine
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.ExerciseRepository
import com.japl.repzi.core.domain.repository.RoutineRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class AddPredefinedRoutineUseCaseTest {

    // Ids and names differ from the keys on purpose: the template must be matched by seed key only.
    private val catalog = listOf(
        Exercise(11, "Sentadilla", MuscleGroup.LEGS, isCustom = false, seedKey = "squat"),
        Exercise(12, "Press de banca", MuscleGroup.CHEST, isCustom = false, seedKey = "bench_press"),
        Exercise(13, "Plancha", MuscleGroup.CORE, isCustom = false, seedKey = "plank"),
        Exercise(14, "Mi ejercicio", MuscleGroup.ARMS, isCustom = true),
    )

    private class FakeExercises(private val list: List<Exercise>) : ExerciseRepository {
        override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> = flowOf(list)
        override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) = Unit
    }

    private class FakeRoutines(existing: List<Routine> = emptyList()) : RoutineRepository {
        val routines = MutableStateFlow(existing)

        override fun observeRoutines(): Flow<List<Routine>> = routines
        override suspend fun getRoutine(id: Long): Routine? = routines.value.firstOrNull { it.id == id }
        override suspend fun saveRoutine(routine: Routine): Long {
            val id = (routines.value.maxOfOrNull { it.id } ?: 0) + 1
            routines.value += routine.copy(id = id)
            return id
        }
        override suspend fun setRestSeconds(routineId: Long, position: Int, restSeconds: Int?) = Unit
        override suspend fun deleteRoutine(id: Long) = Unit
    }

    private val template = PredefinedRoutine("full_body", listOf("squat", "bench_press", "plank"))

    private fun useCase(routines: FakeRoutines, exercises: List<Exercise> = catalog) =
        AddPredefinedRoutineUseCase(FakeExercises(exercises), routines, SaveRoutineUseCase(routines))

    @Test
    fun createsAnOrdinaryRoutineWithTheExercisesInOrder() = runTest {
        val routines = FakeRoutines()

        val result = useCase(routines)(template, "Full body")

        assertEquals(AddPredefinedRoutineResult.Added(1, "Full body"), result)
        val saved = routines.routines.value.single()
        assertEquals("Full body", saved.name)
        assertEquals(listOf(11L, 12L, 13L), saved.exercises.map { it.exercise.id })
    }

    @Test
    fun eachExerciseGetsTheDefaultRestOfItsMuscleGroup() = runTest {
        val routines = FakeRoutines()

        useCase(routines)(template, "Full body")

        // Core rests less than the rest of the groups.
        assertEquals(listOf<Int?>(60, 60, 20), routines.routines.value.single().exercises.map { it.restSeconds })
    }

    @Test
    fun aKeyMissingFromTheCatalogIsSkipped() = runTest {
        val routines = FakeRoutines()
        val withUnknown = PredefinedRoutine("x", listOf("squat", "not_in_this_build", "plank"))

        useCase(routines)(withUnknown, "X")

        assertEquals(listOf(11L, 13L), routines.routines.value.single().exercises.map { it.exercise.id })
    }

    @Test
    fun nothingIsSavedWhenNoExerciseExists() = runTest {
        val routines = FakeRoutines()

        val result = useCase(routines)(PredefinedRoutine("x", listOf("nope")), "X")

        assertEquals(AddPredefinedRoutineResult.Unavailable, result)
        assertTrue(routines.routines.value.isEmpty())
    }

    @Test
    fun customExercisesAreNeverPickedByAKey() = runTest {
        val routines = FakeRoutines()
        val onlyCustom = listOf(Exercise(14, "squat", MuscleGroup.LEGS, isCustom = true))

        val result = useCase(routines, onlyCustom)(template, "Full body")

        assertEquals(AddPredefinedRoutineResult.Unavailable, result)
    }

    @Test
    fun addingItAgainNumbersTheName() = runTest {
        val routines = FakeRoutines()
        val add = useCase(routines)

        add(template, "Full body")
        val second = add(template, "Full body")
        val third = add(template, "Full body")

        assertEquals(AddPredefinedRoutineResult.Added(2, "Full body (2)"), second)
        assertEquals(AddPredefinedRoutineResult.Added(3, "Full body (3)"), third)
    }

    @Test
    fun theNameComparisonIgnoresCaseAndSpaces() = runTest {
        val routines = FakeRoutines(listOf(Routine(5, " full BODY ", emptyList())))

        val result = useCase(routines)(template, "Full body")

        assertEquals("Full body (2)", (result as AddPredefinedRoutineResult.Added).name)
    }
}
