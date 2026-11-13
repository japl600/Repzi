package com.japl.repzi.feature.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.ui.component.EmptyState

@Composable
fun ExercisesRoute(
    onOpenProgress: (exerciseId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesScreen(
        uiState = uiState,
        onGroupSelected = viewModel::onGroupSelected,
        onOpenProgress = onOpenProgress,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisesScreen(
    uiState: ExercisesUiState,
    onGroupSelected: (MuscleGroup?) -> Unit,
    onOpenProgress: (exerciseId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.exercises_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MuscleGroupFilters(selected = uiState.selectedGroup, onSelected = onGroupSelected)

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.exercises.isEmpty() -> EmptyState(stringResource(R.string.exercises_empty))
                else -> LazyColumn {
                    // key estable: evita recomponer y reubicar filas cuando cambia el filtro.
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                val group = stringResource(exercise.muscleGroup.labelRes())
                                val custom = stringResource(R.string.exercises_custom_badge)
                                Text(if (exercise.isCustom) "$group · $custom" else group)
                            },
                            // Only exercises with logged weight have a chart; the others keep the row empty.
                            trailingContent = if (exercise.id in uiState.exercisesWithProgress) {
                                {
                                    IconButton(onClick = { onOpenProgress(exercise.id) }) {
                                        Icon(
                                            painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_trending_up),
                                            contentDescription = stringResource(R.string.exercises_progress, exercise.name),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupFilters(
    selected: MuscleGroup?,
    onSelected: (MuscleGroup?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text(stringResource(R.string.exercises_filter_all)) },
            )
        }
        items(MuscleGroup.entries, key = { it.name }) { group ->
            FilterChip(
                selected = selected == group,
                onClick = { onSelected(group) },
                label = { Text(stringResource(group.labelRes())) },
            )
        }
    }
}

@StringRes
private fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_group_chest
    MuscleGroup.BACK -> R.string.muscle_group_back
    MuscleGroup.LEGS -> R.string.muscle_group_legs
    MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
    MuscleGroup.ARMS -> R.string.muscle_group_arms
    MuscleGroup.CORE -> R.string.muscle_group_core
}
