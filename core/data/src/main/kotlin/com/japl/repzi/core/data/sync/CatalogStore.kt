package com.japl.repzi.core.data.sync

import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.RemoteExerciseWrite
import javax.inject.Inject

/** Where the sync keeps the catalog. An interface so the sync logic is tested on the JVM without Room. */
internal interface CatalogStore {
    /** Gives the built-in exercises the wger id they are known by; idempotent. */
    suspend fun linkBuiltIns()

    /** wger id -> the `last_update_global` stored for it (null for a linked built-in that was never synced). */
    suspend fun remoteVersions(): Map<Long, String?>

    /** Stores one page, atomically. */
    suspend fun apply(writes: List<RemoteExerciseWrite>)
}

internal class RoomCatalogStore @Inject constructor(
    private val dao: ExerciseDao,
) : CatalogStore {

    override suspend fun linkBuiltIns() {
        dao.linkBuiltIns()
    }

    override suspend fun remoteVersions(): Map<Long, String?> =
        dao.remoteVersions().associate { it.remoteId to it.updatedAt }

    override suspend fun apply(writes: List<RemoteExerciseWrite>) = dao.applyRemote(writes)
}
