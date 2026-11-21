package com.japl.repzi.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.repository.DEFAULT_INACTIVITY_DAYS
import com.japl.repzi.core.domain.repository.INACTIVITY_DAYS_RANGE
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
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val restVibration: Flow<Boolean> = preferences.map { it[REST_VIBRATION] ?: true }

    override val restNotification: Flow<Boolean> = preferences.map { it[REST_NOTIFICATION] ?: false }

    override val inactivityReminder: Flow<Boolean> = preferences.map { it[INACTIVITY_REMINDER] ?: false }

    override val inactivityDays: Flow<Int> = preferences.map {
        (it[INACTIVITY_DAYS] ?: DEFAULT_INACTIVITY_DAYS).coerceIn(INACTIVITY_DAYS_RANGE)
    }

    // A value this version does not know (a newer one, or a damaged file) is read as the default.
    override val weightUnit: Flow<WeightUnit> = preferences.map { prefs ->
        WeightUnit.entries.firstOrNull { it.name == prefs[WEIGHT_UNIT] } ?: WeightUnit.KG
    }

    override suspend fun setRestVibration(enabled: Boolean) {
        dataStore.edit { it[REST_VIBRATION] = enabled }
    }

    override suspend fun setRestNotification(enabled: Boolean) {
        dataStore.edit { it[REST_NOTIFICATION] = enabled }
    }

    override suspend fun setInactivityReminder(enabled: Boolean) {
        dataStore.edit { it[INACTIVITY_REMINDER] = enabled }
    }

    override suspend fun setInactivityDays(days: Int) {
        dataStore.edit { it[INACTIVITY_DAYS] = days.coerceIn(INACTIVITY_DAYS_RANGE) }
    }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[WEIGHT_UNIT] = unit.name }
    }

    private companion object {
        val REST_VIBRATION = booleanPreferencesKey("rest_vibration")
        val REST_NOTIFICATION = booleanPreferencesKey("rest_notification")
        val INACTIVITY_REMINDER = booleanPreferencesKey("inactivity_reminder")
        val INACTIVITY_DAYS = intPreferencesKey("inactivity_days")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    }
}
