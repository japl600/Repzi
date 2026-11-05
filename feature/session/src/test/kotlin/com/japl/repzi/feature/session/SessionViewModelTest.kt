package com.japl.repzi.feature.session

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.japl.repzi.core.domain.model.DraftExercise
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WorkoutDraft
import com.japl.repzi.core.domain.repository.DraftRepository
import com.japl.repzi.core.domain.usecase.DeleteWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.SaveSessionUseCase
import com.japl.repzi.core.domain.usecase.SaveWorkoutDraftUseCase
import com.japl.repzi.core.testing.FakeDraftRepository
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    private val routines = FakeRoutineRepository(listOf(Routine(5, "Full body", listOf(squat, bench))))
    private val sessions = FakeSessionRepository()

    private class FakeClock(var current: Instant) : Clock {
        override fun now() = current
    }

    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))

    private val drafts = FakeDraftRepository()

    // Sharing the draft repository between two view models stands for the app being closed and opened again.
    private fun createViewModel(
        routineId: Long = 5,
        draftRepository: DraftRepository = drafts,
    ) = SessionViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ROUTINE_ID_ARG to routineId)),
        getRoutine = GetRoutineUseCase(routines),
        saveSession = SaveSessionUseCase(sessions),
        getDraft = GetWorkoutDraftUseCase(draftRepository, clock),
        saveDraft = SaveWorkoutDraftUseCase(draftRepository),
        deleteDraft = DeleteWorkoutDraftUseCase(draftRepository),
        clock = clock,
    )

    @Test
    fun startsWithOneEmptySetPerRoutineExercise() = runTest {
        val state = createViewModel().state.value

        assertFalse(state.isLoading)
        assertEquals("Full body", state.routineName)
        assertEquals(listOf(squat, bench), state.exercises.map { it.exercise })
        assertTrue(state.exercises.all { it.sets == listOf(SetInput()) })
    }

    @Test
    fun closesWhenTheRoutineNoLongerExists() = runTest {
        val viewModel = createViewModel(routineId = 99)

        viewModel.effects.test { assertEquals(SessionEffect.Close, awaitItem()) }
    }

    @Test
    fun finishingWithoutLoggingAnythingShowsAnErrorAndSavesNothing() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SessionIntent.FinishClicked)

        assertEquals(SessionError.NothingLogged, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSaving)
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun finishingSavesTheTypedSetsAndCloses() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.WeightChanged(0, 0, "100"))
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.onIntent(SessionIntent.SetAdded(0))
        viewModel.onIntent(SessionIntent.RepsChanged(0, 1, "4"))
        // Bench stays empty and must not be saved.
        clock.current = Instant.fromEpochMilliseconds(61_000)

        viewModel.effects.test {
            viewModel.onIntent(SessionIntent.FinishClicked)

            assertEquals(SessionEffect.Close, awaitItem())
        }

        val saved = sessions.saved.single()
        assertEquals("Full body", saved.routineName)
        assertEquals(Instant.fromEpochMilliseconds(1_000), saved.startedAt)
        assertEquals(Instant.fromEpochMilliseconds(61_000), saved.finishedAt)
        assertEquals(
            listOf(LoggedExercise(squat, listOf(LoggedSet(5, 100.0), LoggedSet(4, 100.0)))),
            saved.exercises,
        )
    }

    @Test
    fun aBlankWeightIsSavedAsBodyweight() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(1, 0, "12"))

        viewModel.onIntent(SessionIntent.FinishClicked)

        assertEquals(listOf(LoggedSet(12, 0.0)), sessions.saved.single().exercises.single().sets)
    }

    @Test
    fun backWithoutInputClosesImmediately() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(SessionIntent.BackClicked)

            assertEquals(SessionEffect.Close, awaitItem())
        }
    }

    @Test
    fun backWithInputAsksBeforeDiscarding() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))

        viewModel.effects.test {
            viewModel.onIntent(SessionIntent.BackClicked)
            assertTrue(viewModel.state.value.showDiscardDialog)
            expectNoEvents()

            viewModel.onIntent(SessionIntent.DiscardConfirmed)
            assertEquals(SessionEffect.Close, awaitItem())
        }
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun typingIsSavedAsADraftOfTheRoutine() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SessionIntent.WeightChanged(0, 0, "100"))
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))

        assertEquals(
            WorkoutDraft(
                routineId = 5,
                startedAt = Instant.fromEpochMilliseconds(1_000),
                exercises = listOf(
                    DraftExercise(1, listOf(DraftSet("100", "5"))),
                    DraftExercise(2, listOf(DraftSet("", ""))),
                ),
            ),
            drafts.drafts[5],
        )
    }

    @Test
    fun nothingIsKeptWhileNothingIsTyped() = runTest {
        val viewModel = createViewModel()
        assertTrue(drafts.drafts.isEmpty())

        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        assertEquals(1, drafts.drafts.size)
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, ""))

        assertTrue(drafts.drafts.isEmpty())
    }

    @Test
    fun aNewViewModelResumesTheDraftWithItsStartTime() = runTest {
        val first = createViewModel()
        first.onIntent(SessionIntent.WeightChanged(0, 0, "100"))
        first.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        first.onIntent(SessionIntent.SetAdded(0))
        clock.current = Instant.fromEpochMilliseconds(500_000)

        val resumed = createViewModel()

        assertEquals(first.state.value.exercises, resumed.state.value.exercises)
        assertEquals(listOf(SetInput("100", "5"), SetInput("100", "5")), resumed.state.value.exercises[0].sets)
        resumed.onIntent(SessionIntent.FinishClicked)
        assertEquals(Instant.fromEpochMilliseconds(1_000), sessions.saved.single().startedAt)
    }

    @Test
    fun theDraftIsMatchedByExerciseWhenTheRoutineChanged() = runTest {
        createViewModel().onIntent(SessionIntent.RepsChanged(1, 0, "8"))
        val row = Exercise(3, "Remo", MuscleGroup.BACK, isCustom = false)
        // Squat was removed from the routine and rowing added.
        routines.saveRoutine(Routine(5, "Full body", listOf(bench, row)))

        val exercises = createViewModel().state.value.exercises

        assertEquals(listOf(bench, row), exercises.map { it.exercise })
        assertEquals(listOf(SetInput(reps = "8")), exercises[0].sets)
        assertEquals(listOf(SetInput()), exercises[1].sets)
    }

    @Test
    fun aDraftOfAnotherRoutineIsNotResumed() = runTest {
        routines.saveRoutine(Routine(6, "Legs", listOf(squat)))
        createViewModel(routineId = 6).onIntent(SessionIntent.RepsChanged(0, 0, "5"))

        val state = createViewModel(routineId = 5).state.value

        assertFalse(state.hasInput)
    }

    @Test
    fun aTooOldDraftIsNotResumed() = runTest {
        createViewModel().onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        clock.current = Instant.fromEpochMilliseconds(1_000) + GetWorkoutDraftUseCase.MAX_AGE + 1.minutes

        val state = createViewModel().state.value

        assertFalse(state.hasInput)
        assertTrue(drafts.drafts.isEmpty())
    }

    @Test
    fun finishingDeletesTheDraft() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        assertEquals(1, drafts.drafts.size)

        viewModel.effects.test {
            viewModel.onIntent(SessionIntent.FinishClicked)

            assertEquals(SessionEffect.Close, awaitItem())
        }

        assertEquals(1, sessions.saved.size)
        assertTrue(drafts.drafts.isEmpty())
    }

    @Test
    fun discardingDeletesTheDraft() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.onIntent(SessionIntent.BackClicked)

        viewModel.effects.test {
            viewModel.onIntent(SessionIntent.DiscardConfirmed)

            assertEquals(SessionEffect.Close, awaitItem())
        }

        assertTrue(drafts.drafts.isEmpty())
    }

    @Test
    fun aFailingDraftWriteDoesNotBreakTheWorkout() = runTest {
        val failing = object : DraftRepository {
            override suspend fun getDraft(routineId: Long): WorkoutDraft? = null
            override suspend fun saveDraft(draft: WorkoutDraft) = error("disk full")
            override suspend fun deleteDraft(routineId: Long) = Unit
        }
        val viewModel = createViewModel(draftRepository = failing)

        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.onIntent(SessionIntent.FinishClicked)

        assertEquals(1, sessions.saved.size)
    }

    @Test
    fun keepingTheWorkoutDismissesTheDialog() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.onIntent(SessionIntent.BackClicked)

        viewModel.onIntent(SessionIntent.DiscardDismissed)

        assertFalse(viewModel.state.value.showDiscardDialog)
        assertEquals("5", viewModel.state.value.exercises[0].sets.single().reps)
    }
}
