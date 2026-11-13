package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup

internal fun ExerciseEntity.toDomain(names: ExerciseNames) = Exercise(
    id = id,
    name = names.nameOf(this),
    muscleGroup = MuscleGroup.valueOf(muscleGroup),
    isCustom = isCustom,
)
