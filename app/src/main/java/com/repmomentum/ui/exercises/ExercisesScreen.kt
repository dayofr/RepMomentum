package com.repmomentum.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.R
import com.repmomentum.ui.AppSnackbarHost
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.ui.common.EmptyState
import com.repmomentum.ui.common.LoadingState
import com.repmomentum.ui.common.SectionTitle
import com.repmomentum.ui.common.appViewModelFactory
import com.repmomentum.ui.common.dayTitle
import com.repmomentum.ui.common.exerciseTypeLabel
import com.repmomentum.ui.common.muscleGroupLabel
import com.repmomentum.domain.toLocalDate
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onOpenExercise: (Long) -> Unit,
    onLogExercise: (Long) -> Unit,
    onCreateExercise: () -> Unit,
    viewModel: ExercisesViewModel = viewModel(
        factory = appViewModelFactory { c, handle -> ExercisesViewModel(c.exerciseRepository, handle) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        snackbarHost = { AppSnackbarHost() },
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_exercises)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateExercise,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.action_new_exercise)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.log_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            FilterRow(state.filter, viewModel::setFilter)
            when {
                state.isLoading -> LoadingState()
                state.loadFailed -> EmptyState(
                    Icons.Outlined.ErrorOutline,
                    stringResource(R.string.error_load_title),
                    stringResource(R.string.error_load_message),
                )
                state.isEmpty -> EmptyState(
                    Icons.Outlined.SearchOff,
                    stringResource(R.string.exercises_empty_title),
                    stringResource(R.string.exercises_empty_message),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.favorites.isNotEmpty()) {
                        item(key = "fav") {
                            SectionTitle(stringResource(R.string.exercises_favorites), Modifier.semantics { heading() })
                        }
                        items(state.favorites, key = { it.exercise.id }) { item ->
                            ExerciseRow(item, { onOpenExercise(item.exercise.id) }, { onLogExercise(item.exercise.id) }, { viewModel.toggleFavorite(item.exercise) })
                        }
                    }
                    if (state.others.isNotEmpty()) {
                        item(key = "others") {
                            SectionTitle(stringResource(R.string.exercises_library), Modifier.padding(top = 8.dp).semantics { heading() })
                        }
                        items(state.others, key = { it.exercise.id }) { item ->
                            ExerciseRow(item, { onOpenExercise(item.exercise.id) }, { onLogExercise(item.exercise.id) }, { viewModel.toggleFavorite(item.exercise) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(current: ExerciseFilter, onSelect: (ExerciseFilter) -> Unit) {
    val filters = listOf(ExerciseFilter.All, ExerciseFilter.Favorites) + MuscleGroup.entries.map { ExerciseFilter.Group(it) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = { it.key() }) { filter ->
            FilterChip(
                selected = filter == current,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        when (filter) {
                            ExerciseFilter.All -> stringResource(R.string.filter_all)
                            ExerciseFilter.Favorites -> stringResource(R.string.filter_favorites)
                            is ExerciseFilter.Group -> muscleGroupLabel(filter.group)
                        },
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun ExerciseRow(item: ExerciseItem, onOpen: () -> Unit, onLog: () -> Unit, onToggleFavorite: () -> Unit) {
    val exercise = item.exercise
    val zone = ZoneId.systemDefault()
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val parts = buildList {
                    add(muscleGroupLabel(exercise.muscleGroup))
                    add(exerciseTypeLabel(exercise.exerciseType))
                    if (exercise.isCustom) add(stringResource(R.string.exercise_custom))
                    item.lastUsed?.let { add(stringResource(R.string.exercise_last_used, dayTitle(it.toLocalDate(zone), LocalDate.now(zone)))) }
                }
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconToggleButton(checked = exercise.isFavorite, onCheckedChange = { onToggleFavorite() }) {
                Icon(
                    if (exercise.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(
                        if (exercise.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                        exercise.name,
                    ),
                    tint = if (exercise.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onLog) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.action_log_exercise, exercise.name))
            }
        }
    }
}
