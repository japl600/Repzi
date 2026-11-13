package com.japl.repzi.feature.progress

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.ui.component.EmptyState
import kotlin.math.abs
import kotlin.time.Instant

private const val RECENT_SESSIONS = 3

@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        uiState = uiState,
        onBack = onBack,
        onExerciseSelected = viewModel::onExerciseSelected,
        onRangeSelected = viewModel::onRangeSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgressScreen(
    uiState: ProgressUiState,
    onBack: () -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onRangeSelected: (ProgressRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.progress_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val selected = uiState.selected
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                selected == null -> EmptyState(stringResource(R.string.progress_empty))
                else -> ProgressContent(selected, uiState, onExerciseSelected, onRangeSelected)
            }
        }
    }
}

@Composable
private fun ProgressContent(
    selected: Exercise,
    uiState: ProgressUiState,
    onExerciseSelected: (Long) -> Unit,
    onRangeSelected: (ProgressRange) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExercisePicker(selected, uiState.exercises, onExerciseSelected)
        ChartCard(selected, uiState, onRangeSelected)
        if (uiState.points.isNotEmpty()) {
            Stats(uiState)
            RecentSessions(uiState)
        }
    }
}

@Composable
private fun ExercisePicker(
    selected: Exercise,
    exercises: List<Exercise>,
    onSelected: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.progress_exercise),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(selected.name, style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        expanded = false
                        onSelected(exercise.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    selected: Exercise,
    uiState: ProgressUiState,
    onRangeSelected: (ProgressRange) -> Unit,
) {
    val context = LocalContext.current
    val points = uiState.points
    val latest = points.lastOrNull()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = latest?.let { stringResource(R.string.progress_kg, formatKg(it.maxWeightKg)) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    uiState.changeKg?.let { change ->
                        Text(
                            text = stringResource(R.string.progress_kg, signed(change)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    stringResource(R.string.progress_metric),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (points.isEmpty()) {
                Text(
                    stringResource(R.string.progress_no_data_in_range),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 4.dp),
                )
            } else {
                ProgressChart(
                    points = points,
                    formatDate = { formatDate(context, it) },
                    description = stringResource(
                        R.string.progress_chart_description,
                        selected.name,
                        formatKg(points.last().maxWeightKg),
                        formatKg(points.maxOf { it.maxWeightKg }),
                    ),
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProgressRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.range == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(stringResource(range.labelRes())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Stats(uiState: ProgressUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            label = stringResource(R.string.progress_record),
            value = uiState.record?.let { stringResource(R.string.progress_kg, formatKg(it)) } ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.progress_change),
            value = uiState.changePercent?.let { stringResource(R.string.progress_percent, signed(it.toDouble())) } ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.progress_sessions),
            value = uiState.points.size.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RecentSessions(uiState: ProgressUiState) {
    val context = LocalContext.current
    Column {
        Text(
            stringResource(R.string.progress_recent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        uiState.points.takeLast(RECENT_SESSIONS).reversed().forEach { point ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(context, point.date), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.progress_set, formatKg(point.maxWeightKg), point.reps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@StringRes
private fun ProgressRange.labelRes(): Int = when (this) {
    ProgressRange.MONTH -> R.string.progress_range_month
    ProgressRange.QUARTER -> R.string.progress_range_quarter
    ProgressRange.HALF_YEAR -> R.string.progress_range_half_year
    ProgressRange.ALL -> R.string.progress_range_all
}

/** Locale-aware short date; the year only shows when it is not the current one. */
private fun formatDate(context: android.content.Context, date: Instant): String =
    DateUtils.formatDateTime(
        context,
        date.toEpochMilliseconds(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH,
    )

/** A change with an explicit sign: +5 or -2.5 (zero is shown as +0). */
private fun signed(value: Double): String = (if (value < 0) "-" else "+") + formatKg(abs(value))
