package com.japl.repzi.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/** A routine with its join rows. Room does not order relations, so callers sort by [RoutineExerciseEntity.position]. */
data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "routine_id",
    )
    val items: List<RoutineExerciseWithExercise>,
)

data class RoutineExerciseWithExercise(
    @Embedded val item: RoutineExerciseEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
)
