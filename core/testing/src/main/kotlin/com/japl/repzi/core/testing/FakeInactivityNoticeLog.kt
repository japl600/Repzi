package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.repository.InactivityNoticeLog
import kotlin.time.Instant

class FakeInactivityNoticeLog(initial: Instant? = null) : InactivityNoticeLog {

    private var last: Instant? = initial
    private val _recorded = mutableListOf<Instant>()
    val recorded: List<Instant> get() = _recorded

    override suspend fun lastNoticeFor(): Instant? = last

    override suspend fun record(lastWorkoutAt: Instant) {
        last = lastWorkoutAt
        _recorded += lastWorkoutAt
    }
}
