package com.japl.repzi.core.testing

import com.japl.repzi.core.domain.RestReminderScheduler
import kotlin.time.Instant

/** Records what a feature asked the scheduler to do. */
class FakeRestReminderScheduler : RestReminderScheduler {

    private val _scheduled = mutableListOf<Instant>()
    val scheduled: List<Instant> get() = _scheduled

    var cancelCount = 0
        private set

    override fun schedule(endsAt: Instant) {
        _scheduled += endsAt
    }

    override fun cancel() {
        cancelCount++
    }
}
