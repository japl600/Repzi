package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.database.model.RoutineEntity
import com.japl.repzi.core.database.model.RoutineWithExercises
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.RoutineExercise

internal fun RoutineWithExercises.toDomain(names: ExerciseNames) = Routine(
    id = routine.id,
    name = routine.name,
    // Room returns relations unordered, so the user-defined order is restored from the join table.
    exercises = items.sortedBy { it.item.position }.map { RoutineExercise(it.exercise.toDomain(names), it.item.restSeconds) },
)

internal fun Routine.toEntity() = RoutineEntity(id = id, name = name)
