package com.japl.repzi.core.domain.repository

import com.japl.repzi.core.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow

/** The days the inactivity reminder can wait before it fires. */
val INACTIVITY_DAYS_RANGE: IntRange = 1..14

const val DEFAULT_INACTIVITY_DAYS = 3

/** App-level preferences. */
interface SettingsRepository {

    /** Whether the phone buzzes when a rest countdown ends. On until the user turns it off. */
    val restVibration: Flow<Boolean>

    /** Whether a notification tells the user a rest is over while the app is not on screen. Off until turned on. */
    val restNotification: Flow<Boolean>

    /** Whether a notification reminds the user to train after some days without a workout. Off until turned on. */
    val inactivityReminder: Flow<Boolean>

    /** Days without a workout before the reminder fires; always inside [INACTIVITY_DAYS_RANGE]. */
    val inactivityDays: Flow<Int>

    /** The unit weights are typed and shown in; weights are stored in kilograms whatever this is. Kilograms by default. */
    val weightUnit: Flow<WeightUnit>

    suspend fun setRestVibration(enabled: Boolean)

    suspend fun setRestNotification(enabled: Boolean)

    suspend fun setInactivityReminder(enabled: Boolean)

    /** Values outside [INACTIVITY_DAYS_RANGE] are moved to its nearest end. */
    suspend fun setInactivityDays(days: Int)

    suspend fun setWeightUnit(unit: WeightUnit)
}
