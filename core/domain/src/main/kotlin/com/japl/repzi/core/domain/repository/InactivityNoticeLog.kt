package com.japl.repzi.core.domain.repository

import kotlin.time.Instant

/** Remembers which workout the last "you stopped training" notice was sent for, so a lapse gets one notice. */
interface InactivityNoticeLog {

    /** The finish time of the workout the last notice was sent for, or null if none was sent yet. */
    suspend fun lastNoticeFor(): Instant?

    suspend fun record(lastWorkoutAt: Instant)
}
