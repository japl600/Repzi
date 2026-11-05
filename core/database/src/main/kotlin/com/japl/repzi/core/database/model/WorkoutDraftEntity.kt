package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * The unfinished workout of a routine: one row per routine, so starting over replaces it. It goes away with the
 * routine. [startedAt] is epoch milliseconds.
 */
@Entity(
    tableName = "workout_drafts",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WorkoutDraftEntity(
    @PrimaryKey @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
)
