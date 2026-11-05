package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One set of a draft. Unlike [LoggedSetEntity], [weight] and [reps] are text: a draft holds what is in the text
 * fields, which may be empty or half typed. [exercisePosition] and [setIndex] keep the order, as in logged sets.
 */
@Entity(
    tableName = "draft_sets",
    primaryKeys = ["routine_id", "exercise_position", "set_index"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDraftEntity::class,
            parentColumns = ["routine_id"],
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
    indices = [Index("exercise_id")],
)
data class DraftSetEntity(
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "exercise_position") val exercisePosition: Int,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val weight: String,
    val reps: String,
)
