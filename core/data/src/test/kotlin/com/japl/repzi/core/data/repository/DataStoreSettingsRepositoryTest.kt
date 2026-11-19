package com.japl.repzi.core.data.repository

import java.io.File
import kotlin.test.Test
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

    private fun TestScope.firstScope(job: Job) = CoroutineScope(StandardTestDispatcher(testScheduler) + job)
}
