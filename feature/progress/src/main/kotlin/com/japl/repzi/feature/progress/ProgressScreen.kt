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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.ui.component.EmptyState
import kotlin.math.abs
import kotlin.math.roundToInt
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
        onMetricSelected = viewModel::onMetricSelected,
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
    onMetricSelected: (ProgressMetric) -> Unit,
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
                else -> ProgressContent(selected, uiState, onExerciseSelected, onRangeSelected, onMetricSelected)
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
    onMetricSelected: (ProgressMetric) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExercisePicker(selected, uiState.exercises, onExerciseSelected)
        ChartCard(selected, uiState, onRangeSelected, onMetricSelected)
        if (uiState.series.isNotEmpty()) {
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
    onMetricSelected: (ProgressMetric) -> Unit,
) {
    val context = LocalContext.current
    val metric = uiState.metric
    val series = uiState.series
    val latest = series.lastOrNull()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = latest?.let { metricText(metric, it.value) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    uiState.change?.let { change ->
                        Text(
                            text = metricText(metric, change, signed = true),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    stringResource(metric.descriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (series.isEmpty()) {
                Text(
                    stringResource(R.string.progress_no_data_in_range),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 4.dp),
                )
            } else {
                ProgressChart(
                    points = series,
                    wholeNumbers = !metric.isWeight,
                    formatDate = { formatDate(context, it) },
                    description = stringResource(
                        R.string.progress_chart_description,
                        selected.name,
                        stringResource(metric.descriptionRes()),
                        metricText(metric, series.last().value),
                        metricText(metric, series.maxOf { it.value }),
                    ),
                )
            }

            // An exercise with a single kind of data (bodyweight: only reps) has nothing to choose from.
            if (uiState.availableMetrics.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableMetrics.forEach { option ->
                        FilterChip(
                            selected = metric == option,
                            onClick = { onMetricSelected(option) },
                            label = { Text(stringResource(option.chipLabelRes())) },
                        )
                    }
                }
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
            value = uiState.record?.let { metricText(uiState.metric, it) } ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.progress_change),
            value = uiState.changePercent?.let { stringResource(R.string.progress_percent, signed(it.toDouble())) } ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.progress_sessions),
            value = uiState.series.size.toString(),
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
    val metric = uiState.metric
    // Sessions without a figure for this metric (a bodyweight day when looking at weight) are not listed.
    val recent = uiState.points.filter { metric.valueIn(it) != null }.takeLast(RECENT_SESSIONS).reversed()
    Column {
        Text(
            stringResource(R.string.progress_recent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        recent.forEach { point ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(context, point.date), style = MaterialTheme.typography.bodyMedium)
                Text(
                    // For the max weight the set it came from is more useful than the bare number.
                    text = if (metric == ProgressMetric.MAX_WEIGHT) {
                        stringResource(R.string.progress_set, formatNumber(point.maxWeightKg), point.reps)
                    } else {
                        metricText(metric, metric.valueIn(point) ?: 0.0)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** [value] with its unit: kg for the weight metrics, reps otherwise. With [signed] it gets an explicit + or -. */
@Composable
private fun metricText(metric: ProgressMetric, value: Double, signed: Boolean = false): String {
    val number = if (signed) signed(value) else formatNumber(value)
    return if (metric.isWeight) {
        stringResource(R.string.progress_kg, number)
    } else {
        pluralStringResource(R.plurals.progress_reps, abs(value).roundToInt(), number)
    }
}

@StringRes
private fun ProgressMetric.chipLabelRes(): Int = when (this) {
    ProgressMetric.MAX_WEIGHT -> R.string.progress_metric_chip_max_weight
    ProgressMetric.ESTIMATED_ONE_REP_MAX -> R.string.progress_metric_chip_one_rep_max
    ProgressMetric.VOLUME -> R.string.progress_metric_chip_volume
    ProgressMetric.REPS -> R.string.progress_metric_chip_reps
}

@StringRes
private fun ProgressMetric.descriptionRes(): Int = when (this) {
    ProgressMetric.MAX_WEIGHT -> R.string.progress_metric_max_weight
    ProgressMetric.ESTIMATED_ONE_REP_MAX -> R.string.progress_metric_one_rep_max
    ProgressMetric.VOLUME -> R.string.progress_metric_volume
    ProgressMetric.REPS -> R.string.progress_metric_reps
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
private fun signed(value: Double): String = (if (value < 0) "-" else "+") + formatNumber(abs(value))
