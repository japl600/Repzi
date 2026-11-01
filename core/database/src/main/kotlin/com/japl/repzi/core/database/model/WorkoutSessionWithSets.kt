package com.japl.repzi.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/** A session with its sets. Room does not order relations, so callers sort by exercise position and set index. */
data class WorkoutSessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = LoggedSetEntity::class,
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val sets: List<LoggedSetWithExercise>,
)

data class LoggedSetWithExercise(
    @Embedded val set: LoggedSetEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
)
