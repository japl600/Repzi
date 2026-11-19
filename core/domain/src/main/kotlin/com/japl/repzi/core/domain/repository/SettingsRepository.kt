package com.japl.repzi.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** App-level preferences. */
interface SettingsRepository {

    /** Whether the phone buzzes when a rest countdown ends. On until the user turns it off. */
    val restVibration: Flow<Boolean>

    suspend fun setRestVibration(enabled: Boolean)
}
