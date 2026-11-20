package com.japl.repzi.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.japl.repzi.core.domain.repository.InactivityNoticeLog
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/** Kept in the settings DataStore: it is one number, so it does not earn a table of its own. */
internal class DataStoreInactivityNoticeLog @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : InactivityNoticeLog {

    override suspend fun lastNoticeFor(): Instant? = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()[NOTICED_FOR]
        ?.let(Instant::fromEpochMilliseconds)

    override suspend fun record(lastWorkoutAt: Instant) {
        dataStore.edit { it[NOTICED_FOR] = lastWorkoutAt.toEpochMilliseconds() }
    }

    private companion object {
        val NOTICED_FOR = longPreferencesKey("inactivity_noticed_for")
    }
}
