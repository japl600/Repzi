package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.database.model.LoggedSetEntity
import com.japl.repzi.core.database.model.WorkoutSessionEntity
import com.japl.repzi.core.database.model.WorkoutSessionWithSets
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.LoggedSet
import com.japl.repzi.core.domain.model.WorkoutSession
import kotlin.time.Instant

internal fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    routineName = routineName,
    startedAt = startedAt.toEpochMilliseconds(),
    finishedAt = finishedAt.toEpochMilliseconds(),
)

/** The session id is not known until the insert, so the DAO fills it in. */
internal fun WorkoutSession.toSetEntities(): List<LoggedSetEntity> =
    exercises.flatMapIndexed { exercisePosition, logged ->
        logged.sets.mapIndexed { setIndex, set ->
            LoggedSetEntity(
                sessionId = id,
                exercisePosition = exercisePosition,
                setIndex = setIndex,
                exerciseId = logged.exercise.id,
                reps = set.reps,
                weightKg = set.weightKg,
            )
        }
    }

internal fun WorkoutSessionWithSets.toDomain() = WorkoutSession(
    id = session.id,
    routineName = session.routineName,
    startedAt = Instant.fromEpochMilliseconds(session.startedAt),
    finishedAt = Instant.fromEpochMilliseconds(session.finishedAt),
    // Room returns relations unordered, so the original order is restored from the position columns.
    exercises = sets.groupBy { it.set.exercisePosition }
        .toSortedMap()
        .values
        .map { group ->
            LoggedExercise(
                exercise = group.first().exercise.toDomain(),
                sets = group.sortedBy { it.set.setIndex }.map { LoggedSet(it.set.reps, it.set.weightKg) },
            )
        },
)
