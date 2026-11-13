package com.japl.repzi.feature.history

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.LoggedExercise
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.ui.component.EmptyState

@Composable
fun HistoryRoute(
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onOpenProgress = onOpenProgress,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onDeleteRequested: (WorkoutSession) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    // Without finished workouts there is nothing to chart yet.
                    if (uiState.sessions.isNotEmpty()) {
                        IconButton(onClick = onOpenProgress) {
                            Icon(
                                painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_trending_up),
                                contentDescription = stringResource(R.string.history_progress),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.sessions.isEmpty() -> EmptyState(stringResource(R.string.history_empty))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.sessions, key = { it.id }) { session ->
                        SessionCard(session = session, onDelete = { onDeleteRequested(session) })
                    }
                }
            }
        }
    }

    uiState.sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message, session.routineName)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) { Text(stringResource(R.string.history_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed) { Text(stringResource(R.string.history_cancel)) }
            },
        )
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val date = DateUtils.formatDateTime(
        context,
        session.startedAt.toEpochMilliseconds(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
    )
    val minutes = session.duration.inWholeMinutes.toInt()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.routineName, style = MaterialTheme.typography.titleMedium)
                    Text(date, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_delete))
                }
            }
            Text(
                text = stringResource(
                    R.string.history_summary,
                    pluralStringResource(R.plurals.history_minutes, minutes, minutes),
                    formatKg(session.totalVolumeKg),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.padding(end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                session.exercises.forEach { ExerciseSummary(it) }
            }
        }
    }
}

@Composable
private fun ExerciseSummary(logged: LoggedExercise) {
    Column {
        Text(logged.exercise.name, style = MaterialTheme.typography.titleSmall)
        val sets = logged.sets.map { stringResource(R.string.history_set, formatKg(it.weightKg), it.reps) }
        Text(sets.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
    }
}

/** Shows whole weights without the decimal part: 60 instead of 60.0. */
internal fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()
