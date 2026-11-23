package com.japl.repzi.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.repository.INACTIVITY_DAYS_RANGE
import com.japl.repzi.core.ui.component.label

private const val SOURCE_CODE_URL = "https://github.com/japl600/Repzi"
private const val EXERCISE_DATA_URL = "https://wger.de"

// The per-app language screen only exists on Android 13 and newer.
private val LANGUAGE_SETTINGS_AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
fun SettingsRoute(
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val versionLabel = remember(context) { context.versionLabel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // What the phone says about notifications can change while the user is in the system settings: read it again
    // every time the screen comes back.
    var system by remember { mutableStateOf(context.systemNotificationState()) }
    var permissionDenied by rememberPermissionDenied()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        system = context.systemNotificationState()
        if (system.postAllowed) permissionDenied = false
    }

    // The switch the user turned on while the permission dialog is open, applied only if they allow it.
    var pendingSwitch by rememberPendingNotificationSwitch()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val switch = pendingSwitch
        pendingSwitch = null
        system = context.systemNotificationState()
        if (granted) switch?.turnOn(viewModel) else permissionDenied = true
    }

    // Turning a notification on asks for the permission first (Android 13+); turning it off never does.
    fun changeNotification(enabled: Boolean, switch: NotificationSwitch, apply: (Boolean) -> Unit) {
        val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSwitch = switch
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            apply(enabled)
        }
    }

    val hints = NotificationHints(
        notificationsBlocked = !system.postAllowed &&
            (uiState.restNotification || uiState.inactivityReminder || permissionDenied),
        exactTimingMissing = uiState.restNotification && !system.exactAlarmsAllowed,
    )

    SettingsScreen(
        uiState = uiState,
        onRestVibrationChange = viewModel::onRestVibrationChange,
        onWeightUnitChange = viewModel::onWeightUnitChange,
        hints = hints,
        onRestNotificationChange = {
            changeNotification(it, NotificationSwitch.REST, viewModel::onRestNotificationChange)
        },
        onInactivityReminderChange = {
            changeNotification(it, NotificationSwitch.INACTIVITY, viewModel::onInactivityReminderChange)
        },
        onInactivityDaysChange = viewModel::onInactivityDaysChange,
        onOpenNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivitySafely(intent)
        },
        onOpenExactAlarmSettings = {
            // The hint is only shown from Android 12, where this screen exists.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val uri = Uri.fromParts("package", context.packageName, null)
                context.startActivitySafely(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri))
            }
        },
        versionLabel = versionLabel,
        languageAvailable = LANGUAGE_SETTINGS_AVAILABLE,
        // LocalConfiguration follows the per-app language, so the row updates when the user comes back from changing it.
        currentLanguage = languageLabel(LocalConfiguration.current.locales[0]),
        onOpenLanguage = {
            val uri = Uri.fromParts("package", context.packageName, null)
            context.startActivitySafely(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, uri))
        },
        onOpenSourceCode = {
            context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)))
        },
        onOpenExerciseData = {
            context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(EXERCISE_DATA_URL)))
        },
        onOpenLicenses = onOpenLicenses,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onRestVibrationChange: (Boolean) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit,
    hints: NotificationHints,
    onRestNotificationChange: (Boolean) -> Unit,
    onInactivityReminderChange: (Boolean) -> Unit,
    onInactivityDaysChange: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    versionLabel: String,
    languageAvailable: Boolean,
    currentLanguage: String,
    onOpenLanguage: () -> Unit,
    onOpenSourceCode: () -> Unit,
    onOpenExerciseData: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_section_general))
            if (languageAvailable) {
                NavigationRow(stringResource(R.string.settings_language), onClick = onOpenLanguage, detail = currentLanguage)
            }
            SwitchRow(
                title = stringResource(R.string.settings_rest_vibration),
                checked = uiState.restVibration,
                onCheckedChange = onRestVibrationChange,
            )
            WeightUnitRow(selected = uiState.weightUnit, onSelected = onWeightUnitChange)
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SwitchRow(
                title = stringResource(R.string.settings_rest_notification),
                checked = uiState.restNotification,
                onCheckedChange = onRestNotificationChange,
            )
            if (hints.exactTimingMissing) {
                HintRow(stringResource(R.string.settings_exact_timing_missing), onClick = onOpenExactAlarmSettings)
            }
            SwitchRow(
                title = stringResource(R.string.settings_inactivity_reminder),
                checked = uiState.inactivityReminder,
                onCheckedChange = onInactivityReminderChange,
            )
            if (uiState.inactivityReminder) {
                DaysStepperRow(days = uiState.inactivityDays, onDaysChange = onInactivityDaysChange)
            }
            if (hints.notificationsBlocked) {
                HintRow(stringResource(R.string.settings_notifications_blocked), onClick = onOpenNotificationSettings)
            }
            SectionHeader(stringResource(R.string.settings_section_help))
            NavigationRow(stringResource(R.string.settings_source_code), onClick = onOpenSourceCode)
            NavigationRow(stringResource(R.string.settings_exercise_data), onClick = onOpenExerciseData)
            NavigationRow(stringResource(R.string.settings_licenses), onClick = onOpenLicenses)
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
private fun NavigationRow(title: String, onClick: () -> Unit, detail: String? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = detail?.let { { Text(it) } },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        // The whole row is the toggle, so the switch itself has no click handler of its own.
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A line under a switch that tells the user what the phone is blocking and takes them to where it can be fixed. */
@Composable
private fun HintRow(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun DaysStepperRow(days: Int, onDaysChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(pluralStringResource(R.plurals.settings_inactivity_days, days, days)) },
        trailingContent = {
            Row {
                StepperButton(
                    symbol = "−",
                    description = stringResource(R.string.settings_inactivity_days_less),
                    enabled = days > INACTIVITY_DAYS_RANGE.first,
                    onClick = { onDaysChange(days - 1) },
                )
                StepperButton(
                    symbol = "+",
                    description = stringResource(R.string.settings_inactivity_days_more),
                    enabled = days < INACTIVITY_DAYS_RANGE.last,
                    onClick = { onDaysChange(days + 1) },
                )
            }
        },
    )
}

// A text symbol instead of an icon: minus and plus are not in the core icon set and the extended set is a large library.
@Composable
private fun StepperButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge)
    }
}

/** What the phone currently allows. [postAllowed] means a notification would be shown; the other, an exact alarm. */
private data class SystemNotificationState(val postAllowed: Boolean, val exactAlarmsAllowed: Boolean)

private fun Context.systemNotificationState(): SystemNotificationState {
    val enabled = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    return SystemNotificationState(postAllowed = enabled && permitted, exactAlarmsAllowed = exact)
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

/** Kilograms or pounds: how weights are typed in a workout and shown everywhere. Stored data does not change. */
@Composable
private fun WeightUnitRow(selected: WeightUnit, onSelected: (WeightUnit) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_weight_unit)) },
        trailingContent = {
            SingleChoiceSegmentedButtonRow {
                WeightUnit.entries.forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = unit == selected,
                        onClick = { onSelected(unit) },
                        shape = SegmentedButtonDefaults.itemShape(index, WeightUnit.entries.size),
                    ) {
                        Text(unit.label())
                    }
                }
            }
        },
    )
}
