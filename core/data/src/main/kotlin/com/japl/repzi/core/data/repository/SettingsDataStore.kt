package com.japl.repzi.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The DataStore behind [DataStoreSettingsRepository]. A corrupt file is discarded and replaced by empty
 * preferences, so the defaults apply again. Without this, the first write (which reads the file first) would
 * throw out of the ViewModel.
 */
internal fun createSettingsDataStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler {
        // Deleted first because DataStore replaces a file by renaming over it, which fails on Windows (host tests).
        produceFile().delete()
        emptyPreferences()
    },
    scope = scope,
    produceFile = produceFile,
)
