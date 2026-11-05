package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.database.model.DraftSetEntity
import com.japl.repzi.core.database.model.WorkoutDraftEntity
import com.japl.repzi.core.database.model.WorkoutDraftWithSets
import com.japl.repzi.core.domain.model.DraftExercise
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.domain.model.WorkoutDraft
import kotlin.time.Instant

internal fun WorkoutDraft.toEntity() = WorkoutDraftEntity(
    routineId = routineId,
    startedAt = startedAt.toEpochMilliseconds(),
)

internal fun WorkoutDraft.toSetEntities(): List<DraftSetEntity> =
    exercises.flatMapIndexed { exercisePosition, exercise ->
        exercise.sets.mapIndexed { setIndex, set ->
            DraftSetEntity(
                routineId = routineId,
                exercisePosition = exercisePosition,
                setIndex = setIndex,
                exerciseId = exercise.exerciseId,
                weight = set.weight,
                reps = set.reps,
            )
        }
    }

internal fun WorkoutDraftWithSets.toDomain() = WorkoutDraft(
    routineId = draft.routineId,
    startedAt = Instant.fromEpochMilliseconds(draft.startedAt),
    // Room returns relations unordered, so the original order is restored from the position columns.
    exercises = sets.groupBy { it.exercisePosition }
        .toSortedMap()
        .values
        .map { group ->
            DraftExercise(
                exerciseId = group.first().exerciseId,
                sets = group.sortedBy { it.setIndex }.map { DraftSet(it.weight, it.reps) },
            )
        },
)
