package com.japl.repzi.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.RoutineExercise
import com.japl.repzi.core.domain.repository.SettingsRepository
import com.japl.repzi.core.domain.usecase.DeleteWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.GetRoutineUseCase
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestNotificationUseCase
import com.japl.repzi.core.domain.usecase.ObserveRestVibrationUseCase
import com.japl.repzi.core.domain.usecase.SaveSessionUseCase
import com.japl.repzi.core.domain.usecase.SaveWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.SetExerciseRestUseCase
import com.japl.repzi.core.testing.FakeDraftRepository
import com.japl.repzi.core.testing.FakeRestReminderScheduler
import com.japl.repzi.core.testing.FakeRoutineRepository
import com.japl.repzi.core.testing.FakeSessionRepository
import com.japl.repzi.core.testing.FakeSettingsRepository
import com.japl.repzi.core.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/** The rest timer of the session: what starts it, how it counts down and what the user can do to it. */
class SessionRestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val squat = Exercise(1, "Sentadilla", MuscleGroup.LEGS, isCustom = false)
    private val bench = Exercise(2, "Press de banca", MuscleGroup.CHEST, isCustom = false)

    // Squat rests for a minute, bench has no rest timer.
    private val routines = FakeRoutineRepository(
        listOf(Routine(7, "Full body", listOf(RoutineExercise(squat, 60), RoutineExercise(bench, null)))),
    )
    private val drafts = FakeDraftRepository()
    private val sessions = FakeSessionRepository()
    private val settings = FakeSettingsRepository()

    private class FakeClock(var current: Instant) : Clock {
        override fun now() = current
    }

    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))

    private val viewModels = mutableListOf<SessionViewModel>()

    private val reminders = FakeRestReminderScheduler()

    /** Reads the "notify when a rest ends" setting only once the test lets it, to catch a rest that changes meanwhile. */
    private class GatedSettings(private val gate: CompletableDeferred<Boolean>) :
        SettingsRepository by FakeSettingsRepository() {
        override val restNotification: Flow<Boolean> = flow { emit(gate.await()) }
    }

    private fun createViewModel(notificationSettings: SettingsRepository = settings) = SessionViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ROUTINE_ID_ARG to 7L)),
        getRoutine = GetRoutineUseCase(routines),
        saveSession = SaveSessionUseCase(sessions),
        getDraft = GetWorkoutDraftUseCase(drafts, clock),
        saveDraft = SaveWorkoutDraftUseCase(drafts),
        deleteDraft = DeleteWorkoutDraftUseCase(drafts),
        setExerciseRest = SetExerciseRestUseCase(routines),
        observeRestVibration = ObserveRestVibrationUseCase(settings),
        observeRestNotification = ObserveRestNotificationUseCase(notificationSettings),
        restReminders = reminders,
        clock = clock,
    ).also { viewModels += it }

    /**
     * A countdown that is still running when the test body ends would keep the test scheduler spinning for ever,
     * because the fake clock does not move on its own. Every view model is stopped when the test is over.
     */
    private fun runRestTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            viewModels.forEach { it.viewModelScope.cancel() }
        }
    }

    /** Moves the clock and the coroutine time forward together, as real time would. */
    private fun TestScope.elapse(seconds: Int) {
        clock.current += seconds.seconds
        advanceTimeBy(seconds * 1_000L)
        runCurrent()
    }

    private fun SessionViewModel.tick(exerciseIndex: Int, setIndex: Int = 0) =
        onIntent(SessionIntent.SetDoneToggled(exerciseIndex, setIndex))

    @Test
    fun theLogsCarryTheRestOfTheirExercise() = runRestTest {
        val state = createViewModel().state.value

        assertEquals(listOf<Int?>(60, null), state.exercises.map { it.restSeconds })
    }

    @Test
    fun tickingASetStartsTheRestOfItsExercise() = runRestTest {
        val viewModel = createViewModel()

        viewModel.tick(exerciseIndex = 0)

        assertEquals(RestCountdown(totalSeconds = 60, remainingSeconds = 60), viewModel.state.value.rest)
        assertTrue(viewModel.state.value.exercises[0].sets.single().done)
    }

    @Test
    fun anExerciseWithoutARestTimerDoesNotStartOne() = runRestTest {
        val viewModel = createViewModel()

        viewModel.tick(exerciseIndex = 1)

        assertNull(viewModel.state.value.rest)
        assertTrue(viewModel.state.value.exercises[1].sets.single().done)
    }

    @Test
    fun theCountdownFollowsTheClock() = runRestTest {
        val viewModel = createViewModel()
        viewModel.tick(0)

        elapse(10)

        assertEquals(RestCountdown(60, 50), viewModel.state.value.rest)
    }

    @Test
    fun aPartialSecondIsRoundedUp() = runRestTest {
        val viewModel = createViewModel()
        viewModel.tick(0)

        clock.current += 10_400.milliseconds
        advanceTimeBy(10_400)
        runCurrent()

        assertEquals(50, viewModel.state.value.rest?.remainingSeconds)
    }

    @Test
    fun whenItEndsTheRestClearsAndTheScreenIsToldToBuzz() = runRestTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.tick(0)
            elapse(59)
            expectNoEvents()

            elapse(1)

            assertEquals(SessionEffect.RestFinished, awaitItem())
        }
        assertNull(viewModel.state.value.rest)
    }

    @Test
    fun withVibrationOffTheEndOfTheRestDoesNotBuzz() = runRestTest {
        settings.setRestVibration(false)
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.tick(0)

            elapse(60)

            expectNoEvents()
        }
        assertNull(viewModel.state.value.rest)
    }

    @Test
    fun switchingVibrationOffDuringTheCountdownSilencesIt() = runRestTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.tick(0)
            elapse(30)
            settings.setRestVibration(false)

            elapse(30)

            expectNoEvents()
        }
        assertNull(viewModel.state.value.rest)
    }

    @Test
    fun skippingEndsTheRestWithoutBuzzing() = runRestTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.tick(0)

            viewModel.onIntent(SessionIntent.RestSkipped)
            elapse(120)

            assertNull(viewModel.state.value.rest)
            expectNoEvents()
        }
    }

    @Test
    fun addingTimeExtendsTheRestAndTheBar() = runRestTest {
        val viewModel = createViewModel()
        viewModel.tick(0)
        elapse(10)

        viewModel.onIntent(SessionIntent.RestAdjusted(15))

        assertEquals(RestCountdown(totalSeconds = 75, remainingSeconds = 65), viewModel.state.value.rest)
    }

    @Test
    fun takingTimeAwayShortensTheRest() = runRestTest {
        val viewModel = createViewModel()
        viewModel.tick(0)
        elapse(10)

        viewModel.onIntent(SessionIntent.RestAdjusted(-15))

        assertEquals(35, viewModel.state.value.rest?.remainingSeconds)
    }

    @Test
    fun takingAwayMoreThanIsLeftEndsTheRestSilently() = runRestTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.tick(0)
            elapse(50)

            viewModel.onIntent(SessionIntent.RestAdjusted(-15))

            assertNull(viewModel.state.value.rest)
            expectNoEvents()
        }
    }

    @Test
    fun adjustingWhenNothingIsRunningDoesNothing() = runRestTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SessionIntent.RestAdjusted(15))

        assertNull(viewModel.state.value.rest)
    }

    @Test
    fun tickingAnotherSetRestartsTheCountdown() = runRestTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.SetAdded(0))
        viewModel.tick(0, setIndex = 0)
        elapse(30)

        viewModel.tick(0, setIndex = 1)

        assertEquals(RestCountdown(60, 60), viewModel.state.value.rest)
    }

    @Test
    fun untickingASetNeitherStartsNorStopsTheRest() = runRestTest {
        val viewModel = createViewModel()
        viewModel.tick(0)
        elapse(10)

        viewModel.tick(0)

        assertEquals(false, viewModel.state.value.exercises[0].sets.single().done)
        assertEquals(50, viewModel.state.value.rest?.remainingSeconds)
    }

    @Test
    fun theNewRestIsShownAndSavedInTheRoutine() = runRestTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RestEditRequested(1))
        assertEquals(1, viewModel.state.value.restEditing)

        viewModel.onIntent(SessionIntent.RestChanged(1, 90))

        assertEquals(90, viewModel.state.value.exercises[1].restSeconds)
        assertNull(viewModel.state.value.restEditing)
        assertEquals(listOf<Int?>(60, 90), routines.getRoutine(7)!!.exercises.map { it.restSeconds })
    }

    @Test
    fun theRestTimerCanBeTurnedOffFromTheSession() = runRestTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SessionIntent.RestChanged(0, null))

        assertNull(viewModel.state.value.exercises[0].restSeconds)
        assertNull(routines.getRoutine(7)!!.exercises[0].restSeconds)
        viewModel.tick(0)
        assertNull(viewModel.state.value.rest)
    }

    @Test
    fun aDurationOutsideTheOptionsIsNotSaved() = runRestTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SessionIntent.RestChanged(0, 95))

        assertEquals(60, routines.getRoutine(7)!!.exercises[0].restSeconds)
    }

    @Test
    fun theTickOnASetIsKeptInTheDraftAndResumed() = runRestTest {
        val first = createViewModel()
        first.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        first.tick(0)

        assertEquals(listOf(DraftSet("", "5", done = true)), drafts.drafts[7]!!.exercises[0].sets)

        val resumed = createViewModel()
        assertTrue(resumed.state.value.exercises[0].sets.single().done)
    }

    @Test
    fun theTickDoesNotDecideWhatIsSaved() = runRestTest {
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        // Typed but never ticked: it is still part of the workout.

        viewModel.onIntent(SessionIntent.FinishClicked)

        assertEquals(5, sessions.saved.single().exercises.single().sets.single().reps)
    }

    // The alarm that wakes the user when the rest ends while the app is not on screen.

    private val startOfRest = Instant.fromEpochMilliseconds(1_000)

    @Test
    fun schedulesTheReminderAtTheEndOfTheRestWhenTheSettingIsOn() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()

        viewModel.tick(0)

        assertEquals(listOf(startOfRest + 60.seconds), reminders.scheduled)
    }

    @Test
    fun schedulesNothingWhenTheSettingIsOff() = runRestTest {
        val viewModel = createViewModel()

        viewModel.tick(0)

        assertEquals(emptyList(), reminders.scheduled)
    }

    @Test
    fun adjustingTheRestMovesTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.tick(0)
        elapse(10)

        viewModel.onIntent(SessionIntent.RestAdjusted(15))

        assertEquals(listOf(startOfRest + 60.seconds, startOfRest + 75.seconds), reminders.scheduled)
    }

    @Test
    fun skippingTheRestCancelsTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.tick(0)

        viewModel.onIntent(SessionIntent.RestSkipped)

        assertEquals(1, reminders.cancelCount)
    }

    @Test
    fun takingAwayAllTheRestCancelsTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.tick(0)
        elapse(50)

        viewModel.onIntent(SessionIntent.RestAdjusted(-15))

        assertEquals(1, reminders.cancelCount)
    }

    @Test
    fun finishingTheWorkoutCancelsTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.tick(0)

        viewModel.onIntent(SessionIntent.FinishClicked)

        assertEquals(1, reminders.cancelCount)
    }

    @Test
    fun discardingTheWorkoutCancelsTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.onIntent(SessionIntent.RepsChanged(0, 0, "5"))
        viewModel.tick(0)

        viewModel.onIntent(SessionIntent.BackClicked)
        viewModel.onIntent(SessionIntent.DiscardConfirmed)

        assertEquals(1, reminders.cancelCount)
    }

    @Test
    fun leavingWithoutInputCancelsTheReminder() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.tick(0)

        // Only a tick, nothing typed: going back leaves at once.
        viewModel.onIntent(SessionIntent.BackClicked)

        assertEquals(1, reminders.cancelCount)
    }

    @Test
    fun theReminderIsNotCancelledWhenTheCountdownEndsInTheApp() = runRestTest {
        settings.setRestNotification(true)
        val viewModel = createViewModel()
        viewModel.tick(0)

        elapse(60)

        // The alarm decides by itself, looking at whether the app is on screen. Cancelling here would also cancel it
        // when the app is in the background and the countdown still reaches zero.
        assertEquals(0, reminders.cancelCount)
    }

    @Test
    fun aRestStoppedBeforeTheSettingWasReadIsNotScheduled() = runRestTest {
        val gate = CompletableDeferred<Boolean>()
        val viewModel = createViewModel(notificationSettings = GatedSettings(gate))
        viewModel.tick(0)
        viewModel.onIntent(SessionIntent.RestSkipped)

        gate.complete(true)
        runCurrent()

        assertEquals(emptyList(), reminders.scheduled)
    }
}
