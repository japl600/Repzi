package com.japl.repzi.feature.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.ui.component.EmptyState

@Composable
fun RoutinesRoute(
    onEditRoutine: (Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RoutinesScreen(
        uiState = uiState,
        onCreateRoutine = { onEditRoutine(Routine.NEW_ID) },
        onOpenRoutine = { onEditRoutine(it.id) },
        onStartRoutine = { onStartRoutine(it.id) },
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routines_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.routines_create))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.routines.isEmpty() -> EmptyState(stringResource(R.string.routines_empty))
                else -> LazyColumn {
                    items(uiState.routines, key = { it.id }) { routine ->
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
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.routines_start),
                                        )
                                    }
                                    IconButton(onClick = { onDeleteRequested(routine) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.routines_delete),
                                        )
                                    }
                                }
                            },
                        )
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
