package com.japl.repzi.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.japl.repzi.core.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    // An unreadable file is treated as "no preferences yet", so the defaults apply instead of crashing the screens.
    override val restVibration: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[REST_VIBRATION] ?: true }

    override suspend fun setRestVibration(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[REST_VIBRATION] = enabled }
    }

    private companion object {
        val REST_VIBRATION = booleanPreferencesKey("rest_vibration")
    }
}
