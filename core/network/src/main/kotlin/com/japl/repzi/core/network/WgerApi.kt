package com.japl.repzi.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
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
    @Serializable(with = ExerciseListSerializer::class) val results: List<ExerciseDto> = emptyList(),
)

/**
 * Reads a list one element at a time and drops the ones that cannot be read, so a single malformed record does not
 * cost the whole page. The page size seen by the caller is then the number of readable records, which can be a little
 * smaller than what wger sent; the next request overlaps by that much and the overlap is skipped as already stored.
 */
internal class LenientListSerializer<T>(private val element: KSerializer<T>) : KSerializer<List<T>> {

    private val all = ListSerializer(element)

    override val descriptor: SerialDescriptor = all.descriptor

    override fun serialize(encoder: Encoder, value: List<T>) = all.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<T> {
        val json = decoder as JsonDecoder
        return json.decodeJsonElement().jsonArray.mapNotNull { item ->
            try {
                json.json.decodeFromJsonElement(element, item)
            } catch (_: SerializationException) {
                null
            }
        }
    }
}

internal object ExerciseListSerializer : KSerializer<List<ExerciseDto>> by LenientListSerializer(ExerciseDto.serializer())

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
