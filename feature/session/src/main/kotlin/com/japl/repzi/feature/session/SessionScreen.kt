package com.japl.repzi.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SessionEffect.Close -> onClose()
            }
        }
    }

    // The system back gesture goes through the same intent as the close button.
    BackHandler { viewModel.onIntent(SessionIntent.BackClicked) }

    SessionScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionScreen(
    state: SessionState,
    onIntent: (SessionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.routineName) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SessionIntent.BackClicked) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.session_close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onIntent(SessionIntent.FinishClicked) },
                        enabled = !state.isLoading && !state.isSaving,
                    ) {
                        Text(stringResource(R.string.session_finish))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.error == SessionError.NothingLogged) {
                item(key = "error") {
                    Text(
                        text = stringResource(R.string.session_error_nothing_logged),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            // The same exercise can appear twice in a routine, so the position is the only stable key.
            itemsIndexed(state.exercises, key = { index, _ -> index }) { exerciseIndex, log ->
                ExerciseCard(exerciseIndex = exerciseIndex, log = log, onIntent = onIntent)
            }
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SessionIntent.DiscardDismissed) },
            title = { Text(stringResource(R.string.session_discard_title)) },
            text = { Text(stringResource(R.string.session_discard_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SessionIntent.DiscardConfirmed) }) {
                    Text(stringResource(R.string.session_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SessionIntent.DiscardDismissed) }) {
                    Text(stringResource(R.string.session_discard_keep))
                }
            },
        )
    }
}

@Composable
private fun ExerciseCard(
    exerciseIndex: Int,
    log: ExerciseLog,
    onIntent: (SessionIntent) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(log.exercise.name, style = MaterialTheme.typography.titleMedium)

            log.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    number = setIndex + 1,
                    set = set,
                    canRemove = log.sets.size > 1,
                    onWeightChange = { onIntent(SessionIntent.WeightChanged(exerciseIndex, setIndex, it)) },
                    onRepsChange = { onIntent(SessionIntent.RepsChanged(exerciseIndex, setIndex, it)) },
                    onRemove = { onIntent(SessionIntent.SetRemoved(exerciseIndex, setIndex)) },
                )
            }

            TextButton(onClick = { onIntent(SessionIntent.SetAdded(exerciseIndex)) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.session_add_set))
            }
        }
    }
}

@Composable
private fun SetRow(
    number: Int,
    set: SetInput,
    canRemove: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.session_set_label, number), modifier = Modifier.width(56.dp))
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.session_weight_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = set.reps,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.session_reps_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.session_remove_set))
        }
    }
}
