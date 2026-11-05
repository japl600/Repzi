package com.japl.repzi.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/** A draft with its sets. Room does not order relations, so callers sort by exercise position and set index. */
data class WorkoutDraftWithSets(
    @Embedded val draft: WorkoutDraftEntity,
    @Relation(parentColumn = "routine_id", entityColumn = "routine_id")
    val sets: List<DraftSetEntity>,
)
