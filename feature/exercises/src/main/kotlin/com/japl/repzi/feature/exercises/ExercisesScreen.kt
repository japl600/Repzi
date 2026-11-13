package com.japl.repzi.feature.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    ExercisesScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onGroupSelected = viewModel::onGroupSelected,
        onOpenProgress = onOpenProgress,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisesScreen(
    uiState: ExercisesUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onGroupSelected: (MuscleGroup?) -> Unit,
    onOpenProgress: (exerciseId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.exercises_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(query = searchQuery, onQueryChange = onSearchQueryChange)
            MuscleGroupFilters(selected = uiState.selectedGroup, onSelected = onGroupSelected)

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.exercises.isEmpty() -> EmptyState(
                    if (searchQuery.isBlank()) {
                        stringResource(R.string.exercises_empty)
                    } else {
                        stringResource(R.string.exercises_no_results, searchQuery.trim())
                    },
                )
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
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.exercises_search)) },
        leadingIcon = {
            Icon(painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(com.japl.repzi.core.ui.R.drawable.ic_close),
                        contentDescription = stringResource(R.string.exercises_search_clear),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // The list filters as you type, so the search key only has to get the keyboard out of the way.
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(28.dp),
    )
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
