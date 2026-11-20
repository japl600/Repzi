package com.japl.repzi.core.data.sync

import com.japl.repzi.core.database.model.RemoteExerciseWrite
import com.japl.repzi.core.database.model.TranslationWrite
import com.japl.repzi.core.network.RemoteExercise
import com.japl.repzi.core.network.RemoteLicense
import com.japl.repzi.core.network.WgerClient
import javax.inject.Inject

/** Brings the local catalog up to date with wger. Returns how many exercises it wrote; throws `WgerException`. */
internal fun interface CatalogSyncRunner {
    suspend operator fun invoke(): Int
}

/**
 * Downloads every page of the wger catalog and writes what is new or changed. wger cannot be asked "what changed since"
 * (it ignores ordering and date filters), so each exercise's `last_update_global` is compared with the one stored.
 *
 * Each page is committed on its own. If the run dies halfway, the pages already stored keep their new timestamps and the
 * next run skips them, so nothing is duplicated and no global watermark is needed.
 */
internal class ExerciseCatalogSync @Inject constructor(
    private val client: WgerClient,
    private val store: CatalogStore,
) : CatalogSyncRunner {

    override suspend fun invoke(): Int {
        store.linkBuiltIns()
        val licenses = client.fetchLicenses().associateBy { it.id }
        val versions = store.remoteVersions().toMutableMap()

        var written = 0
        var offset = 0
        do {
            val page = client.fetchPage(offset)
            val changed = page.exercises
                .mapNotNull { it.toWrite(licenses) }
                .filter { versions[it.remoteId] != it.updatedAt }
            if (changed.isNotEmpty()) {
                store.apply(changed)
                changed.forEach { versions[it.remoteId] = it.updatedAt }
                written += changed.size
            }
            offset += page.exercises.size
            // An empty page that still claims there is a next one would loop forever.
        } while (page.hasNext && page.exercises.isNotEmpty())
        return written
    }

    private fun RemoteExercise.toWrite(licenses: Map<Int, RemoteLicense>): RemoteExerciseWrite? {
        val group = muscleGroupOf(category) ?: return null
        if (translations.isEmpty()) return null
        val storedName = (translations.firstOrNull { it.language == "en" } ?: translations.first()).name
        return RemoteExerciseWrite(
            remoteId = id,
            name = storedName,
            muscleGroup = group.name,
            updatedAt = updatedAt,
            translations = translations.map {
                val license = licenses[it.licenseId]
                TranslationWrite(it.language, it.name, it.description, it.author, license?.shortName, license?.url)
            },
        )
    }
}
