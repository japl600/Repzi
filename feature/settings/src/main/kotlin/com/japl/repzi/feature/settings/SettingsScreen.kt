package com.japl.repzi.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private const val SOURCE_CODE_URL = "https://github.com/japl600/Repzi"

// The per-app language screen only exists on Android 13 and newer.
private val LANGUAGE_SETTINGS_AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionLabel = remember(context) { context.versionLabel() }
    SettingsScreen(
        versionLabel = versionLabel,
        languageAvailable = LANGUAGE_SETTINGS_AVAILABLE,
        onOpenLanguage = {
            val uri = Uri.fromParts("package", context.packageName, null)
            context.startActivitySafely(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, uri))
        },
        onOpenSourceCode = {
            context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)))
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    versionLabel: String,
    languageAvailable: Boolean,
    onOpenLanguage: () -> Unit,
    onOpenSourceCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (languageAvailable) {
                SectionHeader(stringResource(R.string.settings_section_general))
                NavigationRow(stringResource(R.string.settings_language), onClick = onOpenLanguage)
            }
            SectionHeader(stringResource(R.string.settings_section_help))
            NavigationRow(stringResource(R.string.settings_source_code), onClick = onOpenSourceCode)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                trailingContent = { Text(versionLabel, style = MaterialTheme.typography.bodyMedium) },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun NavigationRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** "<versionName> (<versionCode>)", read from the installed package because BuildConfig is not shared with features. */
private fun Context.versionLabel(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    return "${info.versionName.orEmpty()} ($code)"
}
