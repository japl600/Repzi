package com.japl.repzi.core.data.mapper

import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup

internal fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = MuscleGroup.valueOf(muscleGroup),
    isCustom = isCustom,
)

internal fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    isCustom = isCustom,
)
