package com.repmomentum.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.R
import com.repmomentum.ui.AppSnackbarHost
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.SetEntry
import com.repmomentum.domain.ExerciseStats
import com.repmomentum.domain.PerformanceMetric
import com.repmomentum.domain.formatDecimal
import com.repmomentum.domain.formatDuration
import com.repmomentum.domain.toLocalDate
import com.repmomentum.ui.common.EmptyState
import com.repmomentum.ui.common.LoadingState
import com.repmomentum.ui.common.MetricLabel
import com.repmomentum.ui.common.PanelCard
import com.repmomentum.ui.common.SectionTitle
import com.repmomentum.ui.common.StatTile
import com.repmomentum.ui.common.appViewModelFactory
import com.repmomentum.ui.common.dayTitle
import com.repmomentum.ui.common.effortLabel
import com.repmomentum.ui.common.formatDate
import com.repmomentum.ui.common.formatTime
import com.repmomentum.ui.common.loadLabel
import com.repmomentum.ui.common.unitLabel
import com.repmomentum.ui.theme.MetricText
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    initialExerciseId: Long?,
    isDetail: Boolean,
    onBack: () -> Unit,
    onLogExercise: (Long) -> Unit,
    onEditExercise: (Long) -> Unit,
    onEditSet: (Long) -> Unit,
    viewModel: ProgressViewModel = viewModel(
        factory = appViewModelFactory { c, handle ->
            ProgressViewModel(initialExerciseId, c.exerciseRepository, c.setRepository, c.clock, handle)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val exercise = state.exercise
    val stats = state.stats
    Scaffold(
        snackbarHost = { AppSnackbarHost() },
        contentWindowInsets = if (isDetail) androidx.compose.material3.ScaffoldDefaults.contentWindowInsets else WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (isDetail) exercise?.name.orEmpty() else stringResource(R.string.nav_progress)) },
                navigationIcon = {
                    if (isDetail) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (exercise != null && exercise.isCustom) {
                        IconButton(onClick = { onEditExercise(exercise.id) }) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit_exercise))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (exercise != null) {
                ExtendedFloatingActionButton(
                    onClick = { onLogExercise(exercise.id) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.action_add_set)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!isDetail && state.candidates.isNotEmpty()) {
                ExerciseSelector(state.candidates, exercise, viewModel::selectExercise)
            }
            when {
                state.isLoading -> LoadingState()
                state.loadFailed -> EmptyState(
                    Icons.Outlined.ErrorOutline,
                    stringResource(R.string.error_load_title),
                    stringResource(R.string.error_load_message),
                )
                exercise == null || stats == null -> EmptyState(
                    Icons.AutoMirrored.Outlined.ShowChart,
                    stringResource(R.string.progress_empty_title),
                    stringResource(R.string.progress_empty_message),
                )
                else -> ProgressContent(exercise, stats, state.history, zone, onEditSet)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelector(candidates: List<Exercise>, selected: Exercise?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.progress_exercise)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            candidates.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        expanded = false
                        onSelect(exercise.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProgressContent(
    exercise: Exercise,
    stats: ExerciseStats,
    history: List<SetEntry>,
    zone: ZoneId,
    onEditSet: (Long) -> Unit,
) {
    val byDay = remember(history) { history.groupBy { it.performedAt.toLocalDate(zone) } }
    val today = LocalDate.now(zone)
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (stats.totalSets == 0) {
            item(key = "empty") {
                EmptyState(
                    Icons.AutoMirrored.Outlined.ShowChart,
                    stringResource(R.string.progress_no_sets_title),
                    stringResource(R.string.progress_no_sets_message),
                )
            }
            return@LazyColumn
        }
        item(key = "highlights") { Highlights(exercise, stats, zone) }
        item(key = "chart") { ChartCard(exercise, stats) }
        item(key = "history") {
            SectionTitle(stringResource(R.string.progress_history), Modifier.padding(top = 8.dp).semantics { heading() })
        }
        byDay.forEach { (date, sets) ->
            item(key = "day-$date") {
                Text(
                    "${dayTitle(date, today)} · ${pluralStringResource(R.plurals.sets_count, sets.size, sets.size)}",
                    style = MetricText.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(sets, key = { it.id }) { set ->
                Surface(
                    onClick = { onEditSet(set.id) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(set.performedAt, zone), style = MetricText.value, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(60.dp))
                        Text(setSummary(exercise, set), style = MetricText.value, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun setSummary(exercise: Exercise, set: SetEntry): String {
    val load = loadLabel(exercise.exerciseType, exercise.loadUnit, set.weight)
    val effort = effortLabel(exercise.exerciseType, set.repetitions)
    return if (load.isEmpty()) effort else "$load × $effort"
}

@Composable
private fun Highlights(exercise: Exercise, stats: ExerciseStats, zone: ZoneId) {
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            stats.lastSet?.let { last ->
                Column {
                    MetricLabel(stringResource(R.string.progress_last))
                    Text(setSummary(exercise, last), style = MetricText.counter)
                    Text(
                        "${formatDate(last.performedAt.toLocalDate(zone))} · ${formatTime(last.performedAt, zone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            stats.bestSet?.let { best ->
                Column {
                    MetricLabel(stringResource(bestLabel(stats.metric)))
                    Text(setSummary(exercise, best), style = MetricText.counter, color = MaterialTheme.colorScheme.primary)
                    Text(
                        formatDate(best.performedAt.toLocalDate(zone)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.progress_best_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.summary_sets), stats.totalSets.toString(), Modifier.weight(1f))
                if (exercise.exerciseType == ExerciseType.TIMED) {
                    StatTile(stringResource(R.string.summary_duration), formatDuration(stats.totalReps), Modifier.weight(1f))
                } else {
                    StatTile(stringResource(R.string.summary_reps), stats.totalReps.toString(), Modifier.weight(1f))
                }
                StatTile(stringResource(R.string.progress_active_days), stats.days.size.toString(), Modifier.weight(1f))
            }
        }
    }
}

private fun bestLabel(metric: PerformanceMetric): Int = when (metric) {
    PerformanceMetric.LOAD -> R.string.progress_best_load
    PerformanceMetric.ADDED_LOAD -> R.string.progress_best_added_load
    PerformanceMetric.REPS -> R.string.progress_best_reps
    PerformanceMetric.DURATION -> R.string.progress_best_duration
}

@Composable
private fun ChartCard(exercise: Exercise, stats: ExerciseStats) {
    val unit = unitLabel(exercise.loadUnit)
    val days = stats.days.takeLast(MAX_CHART_DAYS)
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricLabel(stringResource(chartLabel(stats.metric)))
            if (days.size < 2) {
                Text(
                    stringResource(R.string.progress_chart_not_enough),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val format: (Double) -> String = when (stats.metric) {
                    PerformanceMetric.LOAD, PerformanceMetric.ADDED_LOAD -> { v -> "${formatDecimal(v)} $unit" }
                    PerformanceMetric.REPS -> { v -> v.toInt().toString() }
                    PerformanceMetric.DURATION -> { v -> formatDuration(v.toInt()) }
                }
                ProgressChart(
                    days = days,
                    formatValue = format,
                    description = stringResource(
                        R.string.progress_chart_description,
                        days.size,
                        format(days.first().topValue),
                        format(days.last().topValue),
                    ),
                )
            }
        }
    }
}

private fun chartLabel(metric: PerformanceMetric): Int = when (metric) {
    PerformanceMetric.LOAD -> R.string.progress_chart_load
    PerformanceMetric.ADDED_LOAD -> R.string.progress_chart_added_load
    PerformanceMetric.REPS -> R.string.progress_chart_reps
    PerformanceMetric.DURATION -> R.string.progress_chart_duration
}

private const val MAX_CHART_DAYS = 60
