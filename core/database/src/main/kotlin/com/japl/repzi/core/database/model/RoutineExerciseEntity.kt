package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join table between routines and exercises. [position] keeps the order chosen by the user and, together with
 * [routineId], forms the primary key so the same exercise can appear twice in a routine.
 */
@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routine_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // routine_id is already covered by the primary key; Room asks for an index on every foreign key column.
    indices = [Index("exercise_id")],
)
data class RoutineExerciseEntity(
    @ColumnInfo(name = "routine_id") val routineId: Long,
    val position: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
)
