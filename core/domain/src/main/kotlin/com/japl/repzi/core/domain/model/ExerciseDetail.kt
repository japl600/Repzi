package com.japl.repzi.core.domain.model

/**
 * What the detail screen shows. [description] is HTML written by wger contributors. [author], [licenseName] and
 * [licenseUrl] come from the translation that is shown (the app language, else English); [sourceUrl] is the exercise's
 * page on wger. All of them are null for exercises the user created.
 */
data class ExerciseDetail(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup,
    val isCustom: Boolean,
    val description: String?,
    val author: String?,
    val licenseName: String?,
    val licenseUrl: String?,
    val sourceUrl: String?,
)
