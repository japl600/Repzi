package com.japl.repzi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.japl.repzi.core.database.SeedExercises
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.ExerciseSource
import com.japl.repzi.core.database.model.ExerciseTranslationEntity
import com.japl.repzi.core.database.model.ExerciseWithNames
import com.japl.repzi.core.database.model.RemoteExerciseWrite
import com.japl.repzi.core.database.model.RemoteVersion
import kotlinx.coroutines.flow.Flow

// Abstract class instead of interface: @Transaction needs a method with a body.
@Dao
abstract class ExerciseDao {

    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name")
    abstract fun observeAll(): Flow<List<ExerciseWithNames>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE muscle_group = :muscleGroup ORDER BY name")
    abstract fun observeByMuscleGroup(muscleGroup: String): Flow<List<ExerciseWithNames>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    abstract fun observeById(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercise_translations WHERE exercise_id = :exerciseId")
    abstract fun observeDetailTranslations(exerciseId: Long): Flow<List<ExerciseTranslationEntity>>

    @Insert
    abstract suspend fun insert(exercise: ExerciseEntity): Long

    @Query("SELECT remote_id AS remoteId, remote_updated_at AS updatedAt FROM exercises WHERE remote_id IS NOT NULL")
    abstract suspend fun remoteVersions(): List<RemoteVersion>

    /**
     * Gives each built-in exercise the wger id it is known by ([SeedExercises]) the first time. Returns how many were
     * linked, so it is 0 on every later call. A built-in that already has a remote id is left alone.
     */
    @Transaction
    open suspend fun linkBuiltIns(): Int = SeedExercises.ALL.sumOf { linkSeed(it.key, it.wgerId) }

    /**
     * Stores a page of the wger catalog. A row is found by its wger id: a new one is inserted as a catalog exercise; an
     * existing one gets its timestamp and translations replaced, and, unless it is a built-in, its name and group follow
     * wger. The local id never changes, and a built-in keeps its own name and group. Exercises the user created have no wger id and are never touched.
     */
    @Transaction
    open suspend fun applyRemote(writes: List<RemoteExerciseWrite>) {
        for (write in writes) {
            val existing = idOfRemote(write.remoteId)
            val id = if (existing == null) {
                insert(
                    ExerciseEntity(
                        name = write.name,
                        muscleGroup = write.muscleGroup,
                        isCustom = false,
                        remoteId = write.remoteId,
                        source = ExerciseSource.WGER,
                        remoteUpdatedAt = write.updatedAt,
                    ),
                )
            } else {
                markSynced(write.remoteId, write.updatedAt)
                updateCatalogFields(write.remoteId, write.name, write.muscleGroup)
                existing
            }
            deleteTranslations(id)
            insertTranslations(
                write.translations.map {
                    ExerciseTranslationEntity(id, it.language, it.name, it.description, it.author, it.licenseName, it.licenseUrl)
                },
            )
        }
    }

    @Query("UPDATE OR IGNORE exercises SET remote_id = :remoteId, source = 'wger' WHERE seed_key = :seedKey AND remote_id IS NULL")
    protected abstract suspend fun linkSeed(seedKey: String, remoteId: Long): Int

    // Built-ins (seed_key set) keep their own name and group; only plain catalog rows follow wger.
    @Query("UPDATE exercises SET name = :name, muscle_group = :muscleGroup WHERE remote_id = :remoteId AND seed_key IS NULL")
    protected abstract suspend fun updateCatalogFields(remoteId: Long, name: String, muscleGroup: String)

    @Query("SELECT id FROM exercises WHERE remote_id = :remoteId")
    protected abstract suspend fun idOfRemote(remoteId: Long): Long?

    @Query("UPDATE exercises SET remote_updated_at = :updatedAt WHERE remote_id = :remoteId")
    protected abstract suspend fun markSynced(remoteId: Long, updatedAt: String)

    @Query("DELETE FROM exercise_translations WHERE exercise_id = :exerciseId")
    protected abstract suspend fun deleteTranslations(exerciseId: Long)

    @Insert
    protected abstract suspend fun insertTranslations(translations: List<ExerciseTranslationEntity>)
}
