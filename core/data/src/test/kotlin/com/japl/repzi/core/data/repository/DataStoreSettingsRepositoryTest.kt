package com.japl.repzi.core.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.japl.repzi.core.domain.model.WeightUnit
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(file: File, scope: CoroutineScope) =
        DataStoreSettingsRepository(createSettingsDataStore(scope) { file })

    private fun settingsFile() = File(folder.root, "settings.preferences_pb")

    @Test
    fun restVibrationIsOnByDefault() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        assertTrue(repository.restVibration.first())
    }

    @Test
    fun restVibrationIsPersisted() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setRestVibration(false)

        assertFalse(repository.restVibration.first())
    }

    @Test
    fun valueSurvivesANewRepositoryOnTheSameFile() = runTest {
        val file = settingsFile()
        val firstJob = Job()
        repository(file, firstScope(firstJob)).setRestVibration(false)
        // DataStore allows one instance per file, so the first one has to be released before opening it again.
        firstJob.cancelAndJoin()

        val reopened = repository(file, backgroundScope)

        assertFalse(reopened.restVibration.first())
    }

    @Test
    fun corruptFileFallsBackToOn() = runTest {
        val file = settingsFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5, -1, -2)) }
        val repository = repository(file, backgroundScope)

        assertTrue(repository.restVibration.first())
    }

    @Test
    fun corruptFileCanStillBeWritten() = runTest {
        val file = settingsFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5, -1, -2)) }
        val repository = repository(file, backgroundScope)

        // The first tap on the switch writes, and a write reads the stored file first.
        repository.setRestVibration(false)

        assertFalse(repository.restVibration.first())
    }

    @Test
    fun restNotificationIsOffByDefault() = runTest {
        assertFalse(repository(settingsFile(), backgroundScope).restNotification.first())
    }

    @Test
    fun inactivityReminderIsOffByDefault() = runTest {
        assertFalse(repository(settingsFile(), backgroundScope).inactivityReminder.first())
    }

    @Test
    fun inactivityDaysIsThreeByDefault() = runTest {
        assertEquals(3, repository(settingsFile(), backgroundScope).inactivityDays.first())
    }

    // One write per test: DataStore replaces its file by renaming over it, which fails on Windows from the second write.
    @Test
    fun restNotificationIsPersisted() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setRestNotification(true)

        assertTrue(repository.restNotification.first())
    }

    @Test
    fun inactivityReminderIsPersisted() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setInactivityReminder(true)

        assertTrue(repository.inactivityReminder.first())
    }

    @Test
    fun inactivityDaysArePersisted() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setInactivityDays(7)

        assertEquals(7, repository.inactivityDays.first())
    }

    @Test
    fun inactivityDaysBelowTheRangeAreKeptAtOne() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setInactivityDays(0)

        assertEquals(1, repository.inactivityDays.first())
    }

    @Test
    fun inactivityDaysAboveTheRangeAreKeptAtFourteen() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setInactivityDays(99)

        assertEquals(14, repository.inactivityDays.first())
    }

    @Test
    fun corruptFileFallsBackToTheNewDefaults() = runTest {
        val file = settingsFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5, -1, -2)) }
        val repository = repository(file, backgroundScope)

        assertFalse(repository.restNotification.first())
        assertFalse(repository.inactivityReminder.first())
        assertEquals(3, repository.inactivityDays.first())
    }

    private fun TestScope.firstScope(job: Job) = CoroutineScope(StandardTestDispatcher(testScheduler) + job)

    @Test
    fun weightUnitIsKilogramsByDefault() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        assertEquals(WeightUnit.KG, repository.weightUnit.first())
    }

    @Test
    fun weightUnitIsPersisted() = runTest {
        val repository = repository(settingsFile(), backgroundScope)

        repository.setWeightUnit(WeightUnit.LB)

        assertEquals(WeightUnit.LB, repository.weightUnit.first())
    }

    @Test
    fun anUnknownStoredWeightUnitFallsBackToKilograms() = runTest {
        val dataStore = createSettingsDataStore(backgroundScope) { settingsFile() }
        dataStore.edit { it[stringPreferencesKey("weight_unit")] = "STONE" }

        assertEquals(WeightUnit.KG, DataStoreSettingsRepository(dataStore).weightUnit.first())
    }
}
