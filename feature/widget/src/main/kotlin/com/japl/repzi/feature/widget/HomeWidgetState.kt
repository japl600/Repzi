package com.japl.repzi.feature.widget

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** Everything the home-screen widget shows, already decided: the screen only turns it into text and buttons. */
data class HomeWidgetState(
    val lastWorkout: LastWorkout?,
    val target: WidgetTarget,
    /**
     * The app language the state was computed in. Nothing reads it: it is here so that a language change makes the
     * state differ from the last one, which is what gets the widget redrawn (its texts are chosen while drawing).
     */
    val language: String = "",
)

data class LastWorkout(
    val routineName: String,
    val finishedAt: Instant,
)

/** What the widget's button does. */
sealed interface WidgetTarget {
    /** Begin the routine that is up next. */
    data class Start(val routineId: Long, val routineName: String) : WidgetTarget

    /** Go back to a workout that was left unfinished. */
    data class Resume(val routineId: Long, val routineName: String) : WidgetTarget

    /** There is no routine to start. */
    data object NoRoutines : WidgetTarget
}

/**
 * Decides what the widget shows. [sessions] come newest first and [routines] sorted by name ignoring case, as the use
 * cases deliver them.
 *
 * A workout in progress wins over everything: the button resumes the most recent draft that is not too old and still
 * has its routine. Otherwise the button starts the routine after the one of the last session, in the list's order,
 * wrapping around. Sessions keep only the routine's name, so it is matched ignoring case; when nothing matches (the
 * routine was renamed or deleted, or there are no sessions) the first routine is the one up next.
 */
fun homeWidgetState(
    sessions: List<WorkoutSession>,
    routines: List<Routine>,
    drafts: List<WorkoutDraftInfo>,
    now: Instant,
): HomeWidgetState {
    val last = sessions.firstOrNull()
    val lastWorkout = last?.let { LastWorkout(it.routineName, it.finishedAt) }
    return HomeWidgetState(lastWorkout, target(last, routines, drafts, now))
}

private fun target(
    last: WorkoutSession?,
    routines: List<Routine>,
    drafts: List<WorkoutDraftInfo>,
    now: Instant,
): WidgetTarget {
    val byId = routines.associateBy { it.id }
    val resumable = drafts
        .filter { now - it.startedAt <= GetWorkoutDraftUseCase.MAX_AGE && it.routineId in byId }
        .maxByOrNull { it.startedAt }
    if (resumable != null) {
        return WidgetTarget.Resume(resumable.routineId, byId.getValue(resumable.routineId).name)
    }
    if (routines.isEmpty()) return WidgetTarget.NoRoutines

    val lastIndex = last?.let { session -> routines.indexOfFirst { it.name.equals(session.routineName, ignoreCase = true) } } ?: -1
    val next = routines[(lastIndex + 1) % routines.size]
    return WidgetTarget.Start(next.id, next.name)
}

/** Whole calendar days between [from] and [now] on the calendar of [zone]; 0 for today and for a future moment. */
fun daysAgo(from: Instant, now: Instant, zone: ZoneId): Int {
    val fromDate = from.toJavaInstant().atZone(zone).toLocalDate()
    val today = now.toJavaInstant().atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(fromDate, today).coerceAtLeast(0).toInt()
}
