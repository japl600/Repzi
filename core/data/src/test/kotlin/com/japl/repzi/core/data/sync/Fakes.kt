package com.japl.repzi.core.data.sync

import com.japl.repzi.core.database.model.RemoteExerciseWrite
import com.japl.repzi.core.network.ExercisePage
import com.japl.repzi.core.network.RemoteExercise
import com.japl.repzi.core.network.RemoteLicense
import com.japl.repzi.core.network.RemoteTranslation
import com.japl.repzi.core.network.WgerClient
import com.japl.repzi.core.network.WgerException

/** Serves the given pages by offset (a page starts where the previous one ended). [failOnPage] fails that page once. */
internal class FakeWgerClient(
    var pages: List<List<RemoteExercise>>,
    var licenses: List<RemoteLicense> = listOf(RemoteLicense(2, "CC-BY-SA 4", "https://cc/by-sa/4")),
    private val events: MutableList<String> = mutableListOf(),
) : WgerClient {

    var failOnPage: Int? = null
    var failLicenses = false
    val requestedOffsets = mutableListOf<Int>()

    override suspend fun fetchPage(offset: Int): ExercisePage {
        val index = pages.runningFold(0) { start, page -> start + page.size }.indexOf(offset)
        events += "page:$index"
        requestedOffsets += offset
        if (failOnPage == index) {
            failOnPage = null
            throw WgerException("network down", retryable = true)
        }
        return ExercisePage(pages[index], hasNext = index < pages.lastIndex)
    }

    override suspend fun fetchLicenses(): List<RemoteLicense> {
        events += "licenses"
        if (failLicenses) throw WgerException("no licenses", retryable = true)
        return licenses
    }
}

internal class FakeCatalogStore(private val events: MutableList<String> = mutableListOf()) : CatalogStore {

    val versions = mutableMapOf<Long, String?>()
    val appliedPages = mutableListOf<List<RemoteExerciseWrite>>()
    val applied get() = appliedPages.flatten()

    override suspend fun linkBuiltIns() {
        events += "link"
    }

    override suspend fun remoteVersions(): Map<Long, String?> = versions.toMap()

    override suspend fun apply(writes: List<RemoteExerciseWrite>) {
        events += "apply:${writes.map { it.remoteId }}"
        appliedPages += writes
        writes.forEach { versions[it.remoteId] = it.updatedAt }
    }
}

internal fun translation(
    language: String,
    name: String,
    licenseId: Int = 2,
    author: String? = "author-$language",
) = RemoteTranslation(language, name, "<p>$name</p>", author, licenseId)

internal fun exercise(
    id: Long,
    category: String = "Chest",
    updatedAt: String = "t1",
    translations: List<RemoteTranslation> = listOf(translation("en", "Exercise $id"), translation("es", "Ejercicio $id")),
) = RemoteExercise(id, category, updatedAt, translations)
