package com.japl.repzi.core.network

import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal class RetrofitWgerClient @Inject constructor(
    private val api: WgerApi,
) : WgerClient {

    override suspend fun fetchPage(offset: Int): ExercisePage = call {
        val page = api.exercises(limit = PAGE_SIZE, offset = offset)
        ExercisePage(
            exercises = page.results.map { it.toRemote() },
            hasNext = page.next != null,
        )
    }

    override suspend fun fetchLicenses(): List<RemoteLicense> = call {
        api.licenses().results.map { RemoteLicense(it.id, it.shortName, it.url) }
    }

    /** Turns every failure into a [WgerException]. Cancellation is not caught, so it still cancels the caller. */
    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        val code = e.code()
        throw WgerException("wger answered HTTP $code", retryable = code >= 500 || code == 429, cause = e)
    } catch (e: IOException) {
        throw WgerException("wger could not be reached", retryable = true, cause = e)
    } catch (e: SerializationException) {
        throw WgerException("wger sent an unexpected response", retryable = false, cause = e)
    }

    private fun ExerciseDto.toRemote() = RemoteExercise(
        id = id,
        category = category.name,
        updatedAt = lastUpdateGlobal,
        translations = translations.mapNotNull { dto ->
            val language = LANGUAGES[dto.language] ?: return@mapNotNull null
            RemoteTranslation(language, dto.name, dto.description, dto.licenseAuthor, dto.license)
        },
    )

    private companion object {
        const val PAGE_SIZE = 100

        /** wger language id -> the code the app uses. One line per language, so adding one later is trivial. */
        val LANGUAGES = mapOf(
            2 to "en",
            4 to "es",
        )
    }
}
