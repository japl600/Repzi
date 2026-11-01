package com.japl.repzi.feature.routines

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.Exercise

@Composable
fun RoutineEditorRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableExercises by viewModel.availableExercises.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onFinished()
    }

    RoutineEditorScreen(
        uiState = uiState,
        availableExercises = availableExercises,
        onBack = onFinished,
        onNameChange = viewModel::onNameChange,
        onExerciseToggled = viewModel::onExerciseToggled,
        onExerciseRemoved = viewModel::onExerciseRemoved,
        onExerciseMovedUp = viewModel::onExerciseMovedUp,
        onExerciseMovedDown = viewModel::onExerciseMovedDown,
        onSave = viewModel::onSaveClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineEditorScreen(
    uiState: RoutineEditorUiState,
    availableExercises: List<Exercise>,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onExerciseToggled: (Exercise) -> Unit,
    onExerciseRemoved: (Int) -> Unit,
    onExerciseMovedUp: (Int) -> Unit,
    onExerciseMovedDown: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = if (uiState.isNew) R.string.editor_title_new else R.string.editor_title_edit
                    Text(stringResource(title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.editor_save))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editor_name_label)) },
                    singleLine = true,
                    isError = uiState.error == RoutineEditorError.BlankName,
                    supportingText = {
                        if (uiState.error == RoutineEditorError.BlankName) {
                            Text(stringResource(R.string.editor_error_blank_name))
                        }
                    },
                )
            }

            itemsIndexed(uiState.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onExerciseMovedUp(index) }, enabled = index > 0) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.editor_move_up),
                                )
                            }
                            IconButton(
                                onClick = { onExerciseMovedDown(index) },
                                enabled = index < uiState.exercises.lastIndex,
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.editor_move_down),
                                )
                            }
                            IconButton(onClick = { onExerciseRemoved(index) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.editor_remove),
                                )
                            }
                        }
                    },
                )
            }

            item(key = "add") {
                Column {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.editor_add_exercises))
                    }
                    if (uiState.error == RoutineEditorError.NoExercises) {
                        Text(
                            text = stringResource(R.string.editor_error_no_exercises),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = availableExercises,
            selected = uiState.exercises,
            onToggle = onExerciseToggled,
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    selected: List<Exercise>,
    onToggle: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    // Open fully so the "Done" button below the list is always visible without dragging the sheet.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    modifier = Modifier.clickable { onToggle(exercise) },
                    headlineContent = { Text(exercise.name) },
                    // The row handles the click, so the checkbox is display-only.
                    trailingContent = { Checkbox(checked = exercise in selected, onCheckedChange = null) },
                )
            }
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.editor_picker_done))
        }
    }
}
