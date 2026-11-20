package com.japl.repzi.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.RemoteExerciseWrite
import com.japl.repzi.core.database.model.RemoteVersion
import com.japl.repzi.core.database.model.TranslatedName
import com.japl.repzi.core.database.model.TranslationWrite
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: RepziDatabase
    private lateinit var dao: ExerciseDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepziDatabase::class.java).build()
        dao = database.exerciseDao()
    }

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun insertedExercisesAreObservedOrderedByName() = runTest {
        dao.insert(ExerciseEntity(name = "Sentadilla", muscleGroup = "LEGS", isCustom = false))
        dao.insert(ExerciseEntity(name = "Aperturas", muscleGroup = "CHEST", isCustom = false))

        assertEquals(listOf("Aperturas", "Sentadilla"), dao.observeAll().first().map { it.exercise.name })
    }

    @Test
    fun filtersByMuscleGroup() = runTest {
        dao.insert(ExerciseEntity(name = "Sentadilla", muscleGroup = "LEGS", isCustom = false))
        dao.insert(ExerciseEntity(name = "Aperturas", muscleGroup = "CHEST", isCustom = false))

        assertEquals(listOf("Sentadilla"), dao.observeByMuscleGroup("LEGS").first().map { it.exercise.name })
    }

    private fun translation(language: String, name: String, description: String = "<p>$name</p>") = TranslationWrite(
        language = language,
        name = name,
        description = description,
        author = "author-$language",
        licenseName = "CC-BY-SA 4",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/deed.en",
    )

    private fun write(
        remoteId: Long,
        name: String = "Kettlebell swing",
        group: String = "CORE",
        updatedAt: String = "2026-10-24T16:00:00+02:00",
        translations: List<TranslationWrite> = listOf(translation("en", name), translation("es", "Swing con kettlebell")),
    ) = RemoteExerciseWrite(remoteId, name, group, updatedAt, translations)

    @Test
    fun applyRemoteInsertsANewExerciseWithItsTranslations() = runTest {
        dao.applyRemote(listOf(write(100)))

        val stored = dao.observeAll().first().single()
        assertEquals("wger", stored.exercise.source)
        assertEquals(100L, stored.exercise.remoteId)
        assertEquals("Kettlebell swing", stored.exercise.name)
        assertEquals("CORE", stored.exercise.muscleGroup)
        assertEquals(false, stored.exercise.isCustom)
        assertNull(stored.exercise.seedKey)
        assertEquals(
            setOf(TranslatedName("en", "Kettlebell swing"), TranslatedName("es", "Swing con kettlebell")),
            stored.names.toSet(),
        )
    }

    @Test
    fun applyRemoteWithANewTimestampReplacesTranslationsOfThatExerciseOnly() = runTest {
        dao.applyRemote(listOf(write(100), write(200, name = "Plank", group = "CORE")))
        val idBefore = dao.observeAll().first().first { it.exercise.remoteId == 100L }.exercise.id

        dao.applyRemote(
            listOf(write(100, updatedAt = "2026-11-01T10:00:00+01:00", translations = listOf(translation("en", "Kettlebell swing 2")))),
        )

        val all = dao.observeAll().first()
        val changed = all.first { it.exercise.remoteId == 100L }
        assertEquals(idBefore, changed.exercise.id)
        assertEquals("2026-11-01T10:00:00+01:00", changed.exercise.remoteUpdatedAt)
        assertEquals(listOf(TranslatedName("en", "Kettlebell swing 2")), changed.names)
        assertEquals(2, all.first { it.exercise.remoteId == 200L }.names.size)
        assertEquals(2, all.size)
    }

    @Test
    fun applyRemoteOnALinkedBuiltInKeepsItsNameAndGroup() = runTest {
        dao.insert(ExerciseEntity(name = "Bench press", muscleGroup = "CHEST", isCustom = false, seedKey = "bench_press"))
        dao.linkBuiltIns()

        dao.applyRemote(listOf(write(73, name = "Bench Press (wger)", group = "ARMS")))

        val stored = dao.observeAll().first().single()
        assertEquals("Bench press", stored.exercise.name)
        assertEquals("CHEST", stored.exercise.muscleGroup)
        assertEquals("bench_press", stored.exercise.seedKey)
        assertEquals("2026-10-24T16:00:00+02:00", stored.exercise.remoteUpdatedAt)
        assertEquals(2, stored.names.size)
    }

    @Test
    fun applyRemoteDoesNotTouchCustomExercises() = runTest {
        dao.insert(ExerciseEntity(name = "Mine", muscleGroup = "ARMS", isCustom = true))

        dao.applyRemote(listOf(write(5)))

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
        val custom = all.first { it.exercise.isCustom }
        assertEquals("Mine", custom.exercise.name)
        assertEquals("local", custom.exercise.source)
        assertNull(custom.exercise.remoteId)
        assertEquals(emptyList(), custom.names)
    }

    @Test
    fun remoteVersionsListsOnlyRowsWithARemoteId() = runTest {
        dao.insert(ExerciseEntity(name = "Mine", muscleGroup = "ARMS", isCustom = true))
        dao.applyRemote(listOf(write(100, updatedAt = "t1"), write(200, updatedAt = "t2")))

        assertEquals(
            setOf(RemoteVersion(100L, "t1"), RemoteVersion(200L, "t2")),
            dao.remoteVersions().toSet(),
        )
    }

    @Test
    fun linkBuiltInsLinksSeedsOnceAndIsIdempotent() = runTest {
        dao.insert(ExerciseEntity(name = "Bench press", muscleGroup = "CHEST", isCustom = false, seedKey = "bench_press"))
        dao.insert(ExerciseEntity(name = "Mystery", muscleGroup = "CHEST", isCustom = false, seedKey = "not_a_known_seed"))
        dao.insert(ExerciseEntity(name = "Mine", muscleGroup = "ARMS", isCustom = true))

        assertEquals(1, dao.linkBuiltIns())
        assertEquals(0, dao.linkBuiltIns())

        val linked = dao.observeAll().first().first { it.exercise.seedKey == "bench_press" }.exercise
        assertEquals(73L, linked.remoteId)
        assertEquals("wger", linked.source)
        assertNull(linked.remoteUpdatedAt)
        assertEquals(listOf(RemoteVersion(73L, null)), dao.remoteVersions())
    }

    @Test
    fun remoteIdIsUnique() = runTest {
        dao.insert(ExerciseEntity(name = "A", muscleGroup = "CORE", isCustom = false, remoteId = 1, source = "wger"))

        assertFailsWith<SQLiteConstraintException> {
            dao.insert(ExerciseEntity(name = "B", muscleGroup = "CORE", isCustom = false, remoteId = 1, source = "wger"))
        }
    }

    @Test
    fun observeByIdAndItsTranslationsReturnTheFullRow() = runTest {
        dao.applyRemote(listOf(write(100)))
        val id = dao.observeAll().first().single().exercise.id

        assertEquals("Kettlebell swing", dao.observeById(id).first()?.name)
        val translations = dao.observeDetailTranslations(id).first().associateBy { it.language }
        assertEquals("<p>Kettlebell swing</p>", translations.getValue("en").description)
        assertEquals("author-es", translations.getValue("es").author)
        assertEquals("CC-BY-SA 4", translations.getValue("en").licenseName)
        assertNull(dao.observeById(id + 99).first())
    }

    @Test
    fun deletingAnExerciseDeletesItsTranslations() = runTest {
        dao.applyRemote(listOf(write(100)))
        val id = dao.observeAll().first().single().exercise.id

        database.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = $id")

        assertEquals(emptyList(), dao.observeDetailTranslations(id).first())
    }
}
