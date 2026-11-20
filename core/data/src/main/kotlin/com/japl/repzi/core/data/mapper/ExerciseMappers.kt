package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.database.model.ExerciseSource
import com.japl.repzi.core.database.model.ExerciseWithNames
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup

internal fun ExerciseWithNames.toDomain(names: ExerciseNames) = Exercise(
    id = exercise.id,
    name = names.nameOf(this),
    muscleGroup = MuscleGroup.valueOf(exercise.muscleGroup),
    isCustom = exercise.isCustom,
    seedKey = exercise.seedKey,
    fromCatalog = exercise.source == ExerciseSource.WGER,
)
