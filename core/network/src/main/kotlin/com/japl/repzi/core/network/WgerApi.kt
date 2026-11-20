package com.japl.repzi.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

internal interface WgerApi {

    @GET("exerciseinfo/")
    suspend fun exercises(@Query("limit") limit: Int, @Query("offset") offset: Int): ExercisePageDto

    @GET("license/")
    suspend fun licenses(): LicensePageDto
}

// Only the fields the app uses are declared; the API sends many more.

@Serializable
internal data class ExercisePageDto(
    val next: String? = null,
    val results: List<ExerciseDto> = emptyList(),
)

@Serializable
internal data class ExerciseDto(
    val id: Long,
    @SerialName("last_update_global") val lastUpdateGlobal: String,
    val category: CategoryDto,
    val translations: List<TranslationDto> = emptyList(),
)

@Serializable
internal data class CategoryDto(val name: String)

@Serializable
internal data class TranslationDto(
    val name: String,
    val description: String = "",
    val language: Int,
    /** A license id; `0` when wger does not say. */
    val license: Int = 0,
    @SerialName("license_author") val licenseAuthor: String? = null,
)

@Serializable
internal data class LicensePageDto(val results: List<LicenseDto> = emptyList())

@Serializable
internal data class LicenseDto(
    val id: Int,
    @SerialName("short_name") val shortName: String,
    val url: String,
)
