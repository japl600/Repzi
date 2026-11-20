package com.japl.repzi.core.database.model

/** What the sync stores for one wger exercise. [name] is the English name, used as the stored fallback name. */
data class RemoteExerciseWrite(
    val remoteId: Long,
    val name: String,
    val muscleGroup: String,
    val updatedAt: String,
    val translations: List<TranslationWrite>,
)

data class TranslationWrite(
    val language: String,
    val name: String,
    val description: String,
    val author: String?,
    val licenseName: String?,
    val licenseUrl: String?,
)

/** The stored `last_update_global` of an exercise that has a wger id; null until it is synced the first time. */
data class RemoteVersion(val remoteId: Long, val updatedAt: String?)
