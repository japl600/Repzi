package com.japl.repzi.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.RestTimer
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.component.RestTimePickerDialog
import com.japl.repzi.core.ui.component.formatRestTime
import com.japl.repzi.core.ui.component.label

@Composable
fun SessionRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SessionEffect.Close -> onClose()
                SessionEffect.RestFinished -> context.buzzRestOver()
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
        // The countdown stays in view however far the exercise list is scrolled.
        bottomBar = { state.rest?.let { RestBar(it, onIntent) } },
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
                ExerciseCard(exerciseIndex = exerciseIndex, log = log, weightUnit = state.weightUnit, onIntent = onIntent)
            }
        }
    }

    state.restEditing?.let { index ->
        val log = state.exercises.getOrNull(index)
        if (log != null) {
            RestTimePickerDialog(
                options = RestTimer.options,
                initialSeconds = log.restSeconds,
                onConfirm = { onIntent(SessionIntent.RestChanged(index, it)) },
                onDismiss = { onIntent(SessionIntent.RestEditDismissed) },
            )
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
    weightUnit: WeightUnit,
    onIntent: (SessionIntent) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExerciseHeader(
                name = log.exercise.name,
                onEditRest = { onIntent(SessionIntent.RestEditRequested(exerciseIndex)) },
            )

            log.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    number = setIndex + 1,
                    set = set,
                    weightUnit = weightUnit,
                    canRemove = log.sets.size > 1,
                    onWeightChange = { onIntent(SessionIntent.WeightChanged(exerciseIndex, setIndex, it)) },
                    onRepsChange = { onIntent(SessionIntent.RepsChanged(exerciseIndex, setIndex, it)) },
                    onDoneToggle = { onIntent(SessionIntent.SetDoneToggled(exerciseIndex, setIndex)) },
                    onRemove = { onIntent(SessionIntent.SetRemoved(exerciseIndex, setIndex)) },
                )
                // The rest planned after the set, so the whole plan is visible before training.
                if (log.restSeconds != null && setIndex < log.sets.lastIndex) {
                    PlannedRest(log.restSeconds)
                }
            }

            TextButton(onClick = { onIntent(SessionIntent.SetAdded(exerciseIndex)) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.session_add_set))
            }
        }
    }
}

@Composable
private fun ExerciseHeader(
    name: String,
    onEditRest: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.session_exercise_menu, name))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.session_edit_rest)) },
                    leadingIcon = {
                        Icon(painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_timer), contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        onEditRest()
                    },
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    number: Int,
    set: SetInput,
    weightUnit: WeightUnit,
    canRemove: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDoneToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.session_set_label, number), modifier = Modifier.width(48.dp))
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f),
            label = { Text(weightUnit.label()) },
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
        val doneDescription = stringResource(R.string.session_set_done, number)
        Checkbox(
            checked = set.done,
            onCheckedChange = { onDoneToggle() },
            modifier = Modifier.semantics { contentDescription = doneDescription },
        )
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.session_remove_set))
        }
    }
}

@Composable
private fun PlannedRest(seconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatRestTime(seconds),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The rest in progress: the time left, a progress bar and the buttons to shorten, extend or skip it. */
@Composable
private fun RestBar(
    rest: RestCountdown,
    onIntent: (SessionIntent) -> Unit,
) {
    val step = restAdjustStep(rest.remainingSeconds)
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            LinearProgressIndicator(progress = { rest.progress }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.session_rest_title),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = formatRestTime(rest.remainingSeconds),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                val lessDescription = stringResource(R.string.session_rest_less_description, step)
                TextButton(
                    onClick = { onIntent(SessionIntent.RestAdjusted(-step)) },
                    modifier = Modifier.semantics { contentDescription = lessDescription },
                ) { Text(stringResource(R.string.session_rest_less, step)) }
                val moreDescription = stringResource(R.string.session_rest_more_description, step)
                TextButton(
                    onClick = { onIntent(SessionIntent.RestAdjusted(step)) },
                    modifier = Modifier.semantics { contentDescription = moreDescription },
                ) {
                    Text(stringResource(R.string.session_rest_more, step))
                }
                TextButton(onClick = { onIntent(SessionIntent.RestSkipped) }) {
                    Text(stringResource(R.string.session_rest_skip))
                }
            }
        }
    }
}
