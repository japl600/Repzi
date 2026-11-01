package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One performed set. [exercisePosition] orders the exercises inside the session (the same exercise can appear twice)
 * and [setIndex] orders the sets inside each exercise; together with [sessionId] they form the primary key.
 */
@Entity(
    tableName = "logged_sets",
    primaryKeys = ["session_id", "exercise_position", "set_index"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
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
data class LoggedSetEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_position") val exercisePosition: Int,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val reps: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
)
