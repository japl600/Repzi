package com.japl.repzi.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Before

class WgerClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: WgerClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = RetrofitWgerClient(createWgerApi(server.url("/api/v2/").toString(), createHttpClient()))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun resource(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    private fun enqueueJson(name: String) {
        server.enqueue(
            MockResponse.Builder().code(200).body(resource(name)).addHeader("Content-Type", "application/json").build(),
        )
    }

    @Test
    fun keepsOnlyEnglishAndSpanishTranslations() = runTest {
        enqueueJson("exerciseinfo_page.json")

        val exercise = client.fetchPage(0).exercises.first { it.id == 9L }

        assertEquals(listOf("en", "es"), exercise.translations.map { it.language })
        assertEquals("Abs", exercise.category)
        assertEquals("2026-10-24T16:00:18.357676+02:00", exercise.updatedAt)
    }

    @Test
    fun mapsTranslationFields() = runTest {
        enqueueJson("exerciseinfo_page.json")

        val english = client.fetchPage(0).exercises.first { it.id == 9L }.translations.first()

        assertEquals("2 Handed Kettlebell Swing", english.name)
        assertEquals("<ol><li>Stand up.</li></ol>", english.description)
        assertEquals("deusinvictus", english.author)
        assertEquals(2, english.licenseId)
    }

    @Test
    fun keepsExercisesWithNoAuthor() = runTest {
        enqueueJson("exerciseinfo_page.json")

        val cardio = client.fetchPage(0).exercises.first { it.id == 1115L }

        assertNull(cardio.translations.single().author)
    }

    @Test
    fun hasNextFollowsTheNextLink() = runTest {
        enqueueJson("exerciseinfo_page.json")
        enqueueJson("exerciseinfo_last_page.json")

        assertTrue(client.fetchPage(0).hasNext)
        assertFalse(client.fetchPage(100).hasNext)
    }

    @Test
    fun sendsLimit100TheOffsetAndTheRepziUserAgent() = runTest {
        enqueueJson("exerciseinfo_last_page.json")

        client.fetchPage(200)

        val request = server.takeRequest()
        assertEquals("/api/v2/exerciseinfo/", request.url.encodedPath)
        assertEquals("100", request.url.queryParameter("limit"))
        assertEquals("200", request.url.queryParameter("offset"))
        assertEquals("Repzi", request.headers["User-Agent"])
    }

    @Test
    fun parsesLicenses() = runTest {
        enqueueJson("licenses.json")

        val licenses = client.fetchLicenses()

        assertEquals(listOf(1, 3), licenses.map { it.id })
        assertEquals("CC0", licenses.last().shortName)
        assertEquals("http://creativecommons.org/publicdomain/zero/1.0/", licenses.last().url)
    }

    @Test
    fun ioFailureIsRetryable() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        assertTrue(assertFailsWith<WgerException> { client.fetchPage(0) }.retryable)
    }

    @Test
    fun serverError500IsRetryable() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())

        assertTrue(assertFailsWith<WgerException> { client.fetchPage(0) }.retryable)
    }

    @Test
    fun http429IsRetryable() = runTest {
        server.enqueue(MockResponse.Builder().code(429).build())

        assertTrue(assertFailsWith<WgerException> { client.fetchPage(0) }.retryable)
    }

    @Test
    fun http404IsNotRetryable() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())

        assertFalse(assertFailsWith<WgerException> { client.fetchPage(0) }.retryable)
    }

    @Test
    fun malformedJsonIsNotRetryable() = runTest {
        server.enqueue(
            MockResponse.Builder().code(200).body("<html>not json</html>")
                .addHeader("Content-Type", "application/json").build(),
        )

        assertFalse(assertFailsWith<WgerException> { client.fetchPage(0) }.retryable)
    }
}
