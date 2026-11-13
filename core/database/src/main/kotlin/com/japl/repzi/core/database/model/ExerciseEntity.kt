package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [seedKey] identifies a built-in exercise so its name can be translated when shown. It is null for
 * exercises the user created, whose [name] is shown as typed. For built-in exercises [name] only holds
 * the English fallback.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "seed_key") val seedKey: String? = null,
)
