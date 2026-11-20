package com.japl.repzi.core.network

/** One exercise as wger publishes it. Only English and Spanish translations are kept; the rest never leave this module. */
data class RemoteExercise(
    val id: Long,
    /** wger's category name, e.g. "Chest". Mapping it to a muscle group is the data layer's job. */
    val category: String,
    /** `last_update_global`: it also changes when only a translation is edited. */
    val updatedAt: String,
    val translations: List<RemoteTranslation>,
)

data class RemoteTranslation(
    /** `"en"` or `"es"`. */
    val language: String,
    val name: String,
    /** HTML, written by wger contributors. */
    val description: String,
    val author: String?,
    val licenseId: Int,
)

data class RemoteLicense(val id: Int, val shortName: String, val url: String)

data class ExercisePage(val exercises: List<RemoteExercise>, val hasNext: Boolean)

/** [retryable] tells the caller whether trying again later can help (no network, server trouble) or not (bad response). */
class WgerException(message: String, val retryable: Boolean, cause: Throwable? = null) : Exception(message, cause)

/** The wger catalog, free of any HTTP library type. */
interface WgerClient {
    suspend fun fetchPage(offset: Int): ExercisePage

    suspend fun fetchLicenses(): List<RemoteLicense>
}
