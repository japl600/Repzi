package com.japl.repzi.feature.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.PredefinedRoutine
import com.japl.repzi.core.domain.model.PredefinedRoutines
import com.japl.repzi.core.domain.model.Routine

@Composable
fun RoutinesRoute(
    onEditRoutine: (Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RoutinesEvent.PredefinedAdded -> {
                    // A newer result replaces the one still showing instead of waiting behind it.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.routines_predefined_added, event.name),
                        actionLabel = resources.getString(R.string.routines_predefined_open),
                        // With an action Material keeps the snackbar until it is dismissed; do not leave it there.
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onEditRoutine(event.id)
                }
                RoutinesEvent.PredefinedUnavailable ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.routines_predefined_unavailable))
            }
        }
    }

    RoutinesScreen(
        uiState = uiState,
        onCreateRoutine = { onEditRoutine(Routine.NEW_ID) },
        onOpenRoutine = { onEditRoutine(it.id) },
        onStartRoutine = { onStartRoutine(it.id) },
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onAddPredefined = viewModel::onAddPredefined,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutinesScreen(
    uiState: RoutinesUiState,
    onCreateRoutine: () -> Unit,
    onOpenRoutine: (Routine) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onDeleteRequested: (Routine) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onAddPredefined: (PredefinedRoutine, String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routines_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.routines_create))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                // The bottom space keeps the last card clear of the floating button.
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    if (uiState.routines.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.routines_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            )
                        }
                    } else {
                        item(key = "my-title") { SectionTitle(stringResource(R.string.routines_my_title)) }
                        items(uiState.routines, key = { "routine-${it.id}" }) { routine ->
                            RoutineItem(routine, onOpenRoutine, onStartRoutine, onDeleteRequested)
                        }
                    }
                    item(key = "predefined-title") { SectionTitle(stringResource(R.string.routines_predefined_title)) }
                    items(PredefinedRoutines.all, key = { "predefined-${it.key}" }) { template ->
                        PredefinedRoutineItem(template, onAddPredefined)
                    }
                }
            }
        }
    }

    uiState.routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.routines_delete_title)) },
            text = { Text(stringResource(R.string.routines_delete_message, routine.name)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) { Text(stringResource(R.string.routines_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed) { Text(stringResource(R.string.routines_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun RoutineItem(
    routine: Routine,
    onOpenRoutine: (Routine) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onDeleteRequested: (Routine) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onOpenRoutine(routine) },
        headlineContent = { Text(routine.name) },
        supportingContent = {
            val count = routine.exercises.size
            Text(pluralStringResource(R.plurals.routines_exercise_count, count, count))
        },
        trailingContent = {
            Row {
                IconButton(onClick = { onStartRoutine(routine) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.routines_start))
                }
                IconButton(onClick = { onDeleteRequested(routine) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.routines_delete))
                }
            }
        },
    )
}

@Composable
private fun PredefinedRoutineItem(template: PredefinedRoutine, onAdd: (PredefinedRoutine, String) -> Unit) {
    val texts = PREDEFINED_ROUTINE_TEXTS.getValue(template.key)
    val name = stringResource(texts.name)
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = {
            Column {
                Text(stringResource(texts.description))
                val count = template.exerciseKeys.size
                Text(pluralStringResource(R.plurals.routines_exercise_count, count, count))
            }
        },
        trailingContent = {
            IconButton(onClick = { onAdd(template, name) }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.routines_predefined_add, name),
                )
            }
        },
    )
}
