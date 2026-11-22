package com.japl.repzi.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RemappedOnLanguageChangeTest {

    private class FakeLanguage(initial: String) : AppLanguage {
        val current = MutableStateFlow(initial)
        override fun invoke(): String = current.value
        override val changes: Flow<String> = current
    }

    @Test
    fun theSameRowsAreMappedAgainWhenTheLanguageChanges() = runTest {
        // Room does not emit when only the language changes, so the names would stay in the old one.
        val language = FakeLanguage("en")
        val rows = MutableStateFlow("bench")
        val seen = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            rows.remappedOnLanguageChange(language) { "$it-${language()}" }.toList(seen)
        }

        language.current.value = "es"

        assertEquals(listOf("bench-en", "bench-es"), seen)
        job.cancel()
    }

    @Test
    fun newRowsAreMappedInTheCurrentLanguage() = runTest {
        val language = FakeLanguage("es")
        val rows = MutableStateFlow("a")
        val seen = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            rows.remappedOnLanguageChange(language) { "$it-${language()}" }.toList(seen)
        }

        rows.value = "b"

        assertEquals(listOf("a-es", "b-es"), seen)
        job.cancel()
    }
}
