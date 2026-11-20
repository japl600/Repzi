package com.japl.repzi.core.data.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class DataStoreInactivityNoticeLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun settingsFile() = File(folder.root, "settings.preferences_pb")

    @Test
    fun startsWithoutANotice() = runTest {
        val log = DataStoreInactivityNoticeLog(createSettingsDataStore(backgroundScope) { settingsFile() })

        assertNull(log.lastNoticeFor())
    }

    @Test
    fun recordedNoticeIsReadBack() = runTest {
        val log = DataStoreInactivityNoticeLog(createSettingsDataStore(backgroundScope) { settingsFile() })

        log.record(Instant.fromEpochMilliseconds(5_000))

        assertEquals(Instant.fromEpochMilliseconds(5_000), log.lastNoticeFor())
    }
}
