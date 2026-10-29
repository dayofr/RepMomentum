package com.repmomentum.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.R
import com.repmomentum.ui.AppSnackbarHost
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.LoggedSet
import com.repmomentum.domain.DailySummary
import com.repmomentum.domain.formatDuration
import com.repmomentum.domain.formatGrouped
import com.repmomentum.ui.common.DayPickerDialog
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
import com.repmomentum.ui.logset.SetSavedEvent
import com.repmomentum.ui.theme.MetricText
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun JournalScreen(
    onAddSet: (epochDay: Long) -> Unit,
    onEditSet: (setId: Long) -> Unit,
    onSetRepeated: (SetSavedEvent) -> Unit,
    viewModel: JournalViewModel = viewModel(
        factory = appViewModelFactory { c, handle -> JournalViewModel(c.setRepository, c.clock, handle) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.repeatedSets.collect(onSetRepeated) }
    JournalContent(
        state = state,
        zone = ZoneId.systemDefault(),
        onAddSet = { onAddSet(state.day.toEpochDay()) },
        onEditSet = onEditSet,
        onRepeatSet = viewModel::repeat,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onToday = viewModel::goToToday,
        onSelectDay = viewModel::selectDay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalContent(
    state: JournalUiState,
    zone: ZoneId,
    onAddSet: () -> Unit,
    onEditSet: (Long) -> Unit,
    onRepeatSet: (Long) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        snackbarHost = { AppSnackbarHost() },
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (!state.isToday) {
                        TextButton(onClick = onToday) { Text(stringResource(R.string.day_back_to_today)) }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSet,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_set)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_set"),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            DayNavigator(
                day = state.day,
                today = state.today,
                onPrevious = onPreviousDay,
                onNext = onNextDay,
                onPick = { pickingDay = true },
            )
            when {
                state.isLoading -> LoadingState()
                state.loadFailed -> EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.error_load_title),
                    message = stringResource(R.string.error_load_message),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "summary") { SummaryCard(state.summary) }
                    item(key = "title") {
                        SectionTitle(
                            text = pluralStringResource(R.plurals.journal_sets_title, state.sets.size, state.sets.size),
                            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                        )
                    }
                    if (state.sets.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                icon = Icons.Outlined.EventNote,
                                title = stringResource(R.string.journal_empty_title),
                                message = stringResource(R.string.journal_empty_message),
                            )
                        }
                    }
                    items(state.sets, key = { it.set.id }) { logged ->
                        SetRow(
                            logged = logged,
                            zone = zone,
                            onClick = { onEditSet(logged.set.id) },
                            onRepeat = { onRepeatSet(logged.set.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
    if (pickingDay) {
        DayPickerDialog(
            initial = state.day,
            maxDate = state.today,
            onDismiss = { pickingDay = false },
            onConfirm = {
                pickingDay = false
                onSelectDay(it)
            },
        )
    }
}

@Composable
private fun DayNavigator(day: LocalDate, today: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.day_previous))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = stringResource(R.string.day_pick), onClick = onPick)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dayTitle(day, today), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(formatDate(day), style = MetricText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNext, enabled = day < today) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.day_next))
        }
    }
}

@Composable
private fun SummaryCard(summary: DailySummary) {
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.summary_sets), summary.setCount.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.summary_reps), summary.totalReps.toString(), Modifier.weight(1f))
                VolumeTile(summary.volumeByUnit, hasSets = summary.setCount > 0, Modifier.weight(1.3f))
            }
            if (summary.totalSeconds > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricLabel(stringResource(R.string.summary_duration))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDuration(summary.totalSeconds), style = MetricText.value, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun VolumeTile(volumes: Map<LoadUnit, Double>, hasSets: Boolean, modifier: Modifier) {
    val label = stringResource(R.string.summary_volume)
    when (volumes.size) {
        0 -> StatTile(label, "—", modifier, caption = if (hasSets) stringResource(R.string.summary_volume_none) else null)
        1 -> {
            val (unit, value) = volumes.entries.first()
            StatTile(label, formatGrouped(value), modifier, caption = unitLabel(unit))
        }
        else -> {
            // Pas d'addition kg + lb : une valeur par unité.
            val kg = volumes[LoadUnit.KG] ?: 0.0
            val lb = volumes[LoadUnit.LB] ?: 0.0
            StatTile(
                label,
                "${formatGrouped(kg)} ${unitLabel(LoadUnit.KG)}",
                modifier,
                caption = "+ ${formatGrouped(lb)} ${unitLabel(LoadUnit.LB)}",
            )
        }
    }
}

@Composable
private fun SetRow(logged: LoggedSet, zone: ZoneId, onClick: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier) {
    val (set, exercise) = logged
    val load = loadLabel(exercise.exerciseType, exercise.loadUnit, set.weight)
    val effort = effortLabel(exercise.exerciseType, set.repetitions)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatTime(set.performedAt, zone),
                style = MetricText.value,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(60.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (load.isEmpty()) effort else "$load × $effort",
                    style = MetricText.value,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!set.notes.isNullOrBlank()) {
                    Text(
                        set.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRepeat) {
                Icon(
                    Icons.Outlined.Replay,
                    contentDescription = stringResource(R.string.action_repeat_set, exercise.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
