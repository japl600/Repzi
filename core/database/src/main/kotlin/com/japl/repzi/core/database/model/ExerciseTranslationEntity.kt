package com.japl.repzi.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * One exercise in one language. [language] is an ISO code (`"en"`, `"es"`). [description] is the HTML wger publishes.
 * The author and the license belong to the translation, because that is how wger provides them.
 */
@Entity(
    tableName = "exercise_translations",
    primaryKeys = ["exercise_id", "language"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseTranslationEntity(
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val language: String,
    val name: String,
    val description: String,
    val author: String?,
    @ColumnInfo(name = "license_name") val licenseName: String?,
    @ColumnInfo(name = "license_url") val licenseUrl: String?,
)
