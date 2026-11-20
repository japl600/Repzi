package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [seedKey] identifies a built-in exercise so its name can be translated when shown. It is null for
 * exercises the user created, whose [name] is shown as typed. For built-in exercises [name] only holds
 * the English fallback.
 *
 * [remoteId] is the wger id, set on exercises that come from (or were matched with) the wger catalog; it is unique.
 * [source] is [ExerciseSource.LOCAL] or [ExerciseSource.WGER]. [remoteUpdatedAt] is wger's `last_update_global` as it
 * was when the exercise was last synced, so the next sync can skip what has not changed.
 */
@Entity(tableName = "exercises", indices = [Index(value = ["remote_id"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "seed_key") val seedKey: String? = null,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(defaultValue = "'local'") val source: String = ExerciseSource.LOCAL,
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: String? = null,
)

object ExerciseSource {
    const val LOCAL = "local"
    const val WGER = "wger"
}
