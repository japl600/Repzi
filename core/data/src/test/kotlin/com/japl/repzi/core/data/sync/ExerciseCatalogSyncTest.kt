package com.japl.repzi.core.data.sync

import com.japl.repzi.core.network.WgerException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ExerciseCatalogSyncTest {

    private val events = mutableListOf<String>()
    private val store = FakeCatalogStore(events)

    private fun sync(client: FakeWgerClient) = ExerciseCatalogSync(client, store)

    @Test
    fun linksBuiltInsOnceTheFirstPageIsInHandAndBeforeWritingIt() = runTest {
        sync(FakeWgerClient(listOf(listOf(exercise(1))), events = events))()

        // Linking has to come before the write, or the wger copy of a built-in would be added next to it.
        assertTrue(events.indexOf("page:0") < events.indexOf("link"))
        assertTrue(events.indexOf("link") < events.indexOf("apply:[1]"))
    }

    @Test
    fun nothingIsLinkedWhenTheLicensesCannotBeFetched() = runTest {
        // A built-in counts as wger content, which is what shows the attribution: with nothing downloaded it must not.
        val client = FakeWgerClient(listOf(listOf(exercise(1))), events = events)
        client.failLicenses = true

        assertFailsWith<WgerException> { sync(client)() }

        assertTrue("link" !in events)
    }

    @Test
    fun nothingIsLinkedWhenTheFirstPageCannotBeFetched() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1))), events = events)
        client.failOnPage = 0

        assertFailsWith<WgerException> { sync(client)() }

        assertTrue("link" !in events)
    }

    @Test
    fun firstRunWritesEveryNonCardioExerciseAndSkipsCardio() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1, "Chest"), exercise(2, "Cardio"), exercise(3, "Abs"))))

        val written = sync(client)()

        assertEquals(2, written)
        assertEquals(listOf(1L, 3L), store.applied.map { it.remoteId })
    }

    @Test
    fun mapsTheFieldsOfAnExercise() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(7, "Back", updatedAt = "2026-10-24"))))

        sync(client)()

        val write = store.applied.single()
        assertEquals("BACK", write.muscleGroup)
        assertEquals("2026-10-24", write.updatedAt)
        val spanish = write.translations.first { it.language == "es" }
        assertEquals("Ejercicio 7", spanish.name)
        assertEquals("<p>Ejercicio 7</p>", spanish.description)
        assertEquals("author-es", spanish.author)
    }

    @Test
    fun usesTheEnglishNameAsTheStoredName() = runTest {
        val client = FakeWgerClient(
            listOf(
                listOf(
                    exercise(1, translations = listOf(translation("es", "Plancha"), translation("en", "Plank"))),
                    exercise(2, translations = listOf(translation("es", "Solo español"))),
                ),
            ),
        )

        sync(client)()

        assertEquals(listOf("Plank", "Solo español"), store.applied.map { it.name })
    }

    @Test
    fun secondRunWithNothingChangedWritesNothing() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1), exercise(2))))
        val sync = sync(client)

        assertEquals(2, sync())
        assertEquals(0, sync())

        assertEquals(1, store.appliedPages.size)
    }

    @Test
    fun aChangedTimestampRewritesOnlyThatExercise() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1), exercise(2))))
        val sync = sync(client)
        sync()

        // Only a translation was edited on wger: the exercise's own fields are the same, its global timestamp is new.
        client.pages = listOf(listOf(exercise(1), exercise(2, updatedAt = "t2")))
        val written = sync()

        assertEquals(1, written)
        assertEquals(listOf(1L, 2L, 2L), store.applied.map { it.remoteId })
        assertEquals("t2", store.versions[2L])
    }

    @Test
    fun aLinkedBuiltInWithoutATimestampIsWrittenOnTheFirstRun() = runTest {
        store.versions[73L] = null
        val client = FakeWgerClient(listOf(listOf(exercise(73))))

        assertEquals(1, sync(client)())
    }

    @Test
    fun resolvesLicenseNameAndUrlFromTheLicenseList() = runTest {
        val client = FakeWgerClient(
            listOf(listOf(exercise(1, translations = listOf(translation("en", "A", licenseId = 2))))),
        )

        sync(client)()

        val stored = store.applied.single().translations.single()
        assertEquals("CC-BY-SA 4", stored.licenseName)
        assertEquals("https://cc/by-sa/4", stored.licenseUrl)
    }

    @Test
    fun unknownLicenseIdLeavesLicenseFieldsNull() = runTest {
        val client = FakeWgerClient(
            listOf(listOf(exercise(1, translations = listOf(translation("en", "A", licenseId = 99))))),
        )

        sync(client)()

        val stored = store.applied.single().translations.single()
        assertNull(stored.licenseName)
        assertNull(stored.licenseUrl)
    }

    @Test
    fun interruptedRunKeepsCommittedPagesAndTheNextRunWritesOnlyTheRest() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1), exercise(2)), listOf(exercise(3), exercise(4))))
        client.failOnPage = 1
        val sync = sync(client)

        assertFailsWith<WgerException> { sync() }
        assertEquals(listOf(1L, 2L), store.applied.map { it.remoteId })

        assertEquals(2, sync())
        assertEquals(listOf(1L, 2L, 3L, 4L), store.applied.map { it.remoteId })
        assertEquals(listOf(0, 2, 0, 2), client.requestedOffsets)
    }

    @Test
    fun anExerciseWithNoTranslationIsSkipped() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1, translations = emptyList()), exercise(2))))

        sync(client)()

        assertEquals(listOf(2L), store.applied.map { it.remoteId })
    }

    @Test
    fun licenseFailureStopsBeforeAnyWrite() = runTest {
        val client = FakeWgerClient(listOf(listOf(exercise(1))))
        client.failLicenses = true

        assertFailsWith<WgerException> { sync(client)() }

        assertEquals(emptyList(), store.applied)
        assertEquals(emptyList(), client.requestedOffsets)
    }

    @Test
    fun anEmptyPageThatClaimsAnotherOneEndsTheRun() = runTest {
        val client = FakeWgerClient(listOf(emptyList(), listOf(exercise(9))))

        assertEquals(0, sync(client)())
    }
}
