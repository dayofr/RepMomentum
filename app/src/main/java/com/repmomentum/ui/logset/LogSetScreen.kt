package com.repmomentum.ui.logset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.R
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.domain.formatDecimal
import com.repmomentum.domain.toLocalDate
import com.repmomentum.domain.toLocalTime
import com.repmomentum.ui.common.DayPickerDialog
import com.repmomentum.ui.common.EmptyState
import com.repmomentum.ui.common.LoadingState
import com.repmomentum.ui.common.MetricLabel
import com.repmomentum.ui.common.PanelCard
import com.repmomentum.ui.common.StepperField
import com.repmomentum.ui.common.TimePickerDialog
import com.repmomentum.ui.common.appViewModelFactory
import com.repmomentum.ui.common.dayTitle
import com.repmomentum.ui.common.effortLabel
import com.repmomentum.ui.common.exerciseTypeLabel
import com.repmomentum.ui.common.formatTime
import com.repmomentum.ui.common.inputErrorLabel
import com.repmomentum.ui.common.loadLabel
import com.repmomentum.ui.common.muscleGroupLabel
import com.repmomentum.ui.common.unitLabel
import com.repmomentum.ui.LogSetRoute
import com.repmomentum.ui.theme.MetricText
import java.time.ZoneId

private val REP_PRESETS = listOf(5, 8, 10, 12, 15)
private val SECOND_PRESETS = listOf(30, 45, 60, 90)
private const val WIDE_LAYOUT_DP = 600

@Composable
fun LogSetScreen(
    route: LogSetRoute,
    onClose: () -> Unit,
    onSaved: (SetSavedEvent) -> Unit,
    onDeleted: () -> Unit,
    viewModel: LogSetViewModel = viewModel(
        factory = appViewModelFactory { c, handle ->
            LogSetViewModel(route, c.exerciseRepository, c.setRepository, c.settingsRepository, c.clock, handle)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LogSetEvent.Saved -> onSaved(event.event)
                LogSetEvent.Deleted -> onDeleted()
            }
        }
    }
    LogSetContent(
        state = state,
        zone = ZoneId.systemDefault(),
        actions = LogSetActions(
            onClose = onClose,
            onSelectExercise = viewModel::selectExercise,
            onBrowse = viewModel::setBrowsing,
            onQuery = viewModel::setQuery,
            onWeight = viewModel::setWeight,
            onStepWeight = viewModel::stepWeight,
            onReps = viewModel::setReps,
            onStepReps = viewModel::stepReps,
            onNotes = viewModel::setNotes,
            onShowNotes = viewModel::showNotes,
            onDate = viewModel::setDate,
            onTime = viewModel::setTime,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        ),
    )
}

class LogSetActions(
    val onClose: () -> Unit,
    val onSelectExercise: (Long) -> Unit,
    val onBrowse: (Boolean) -> Unit,
    val onQuery: (String) -> Unit,
    val onWeight: (String) -> Unit,
    val onStepWeight: (Int) -> Unit,
    val onReps: (String) -> Unit,
    val onStepReps: (Int) -> Unit,
    val onNotes: (String) -> Unit,
    val onShowNotes: () -> Unit,
    val onDate: (java.time.LocalDate) -> Unit,
    val onTime: (java.time.LocalTime) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSetContent(state: LogSetUiState, zone: ZoneId, actions: LogSetActions) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // Paysage ou grand écran : deux colonnes, le bouton Enregistrer suit la saisie.
    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_DP
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.log_title_edit else R.string.log_title_new)) },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    if (state.isEdit && !state.notFound) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete_set))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!wide && !state.isLoading && !state.notFound) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    SaveBar(state, actions, Modifier.navigationBarsPadding().imePadding().padding(16.dp))
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.notFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.log_not_found_title),
                message = stringResource(R.string.log_not_found_message),
                modifier = Modifier.padding(padding),
            )
            wide -> Row(
                modifier = Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val exercise = state.selected
                ScrollColumn(Modifier.weight(1f)) {
                    ExercisePicker(state, actions)
                    if (exercise != null && !state.browsing) SelectedHeader(exercise, state, zone)
                }
                Column(Modifier.weight(1f).navigationBarsPadding()) {
                    ScrollColumn(Modifier.weight(1f)) {
                        if (exercise != null && !state.browsing) {
                            InputFields(exercise, state, actions)
                            TimeRow(state, zone, actions)
                            NotesField(state, actions)
                        }
                    }
                    SaveBar(state, actions, Modifier.padding(vertical = 8.dp))
                }
            }
            else -> ScrollColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                ExercisePicker(state, actions)
                val exercise = state.selected
                if (exercise != null && !state.browsing) {
                    SelectedHeader(exercise, state, zone)
                    InputFields(exercise, state, actions)
                    TimeRow(state, zone, actions)
                    NotesField(state, actions)
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_set_title)) },
            text = { Text(stringResource(R.string.delete_set_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ScrollColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun SaveBar(state: LogSetUiState, actions: LogSetActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (state.saveFailed) {
            Text(
                stringResource(R.string.error_save),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_set"),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (state.selected == null) R.string.log_pick_first else R.string.action_save_set),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePicker(state: LogSetUiState, actions: LogSetActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricLabel(stringResource(R.string.log_exercise))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.quickPicks.forEach { exercise ->
                val selected = exercise.id == state.selected?.id
                FilterChip(
                    selected = selected,
                    onClick = { actions.onSelectExercise(exercise.id) },
                    label = { Text(exercise.name) },
                    leadingIcon = when {
                        selected -> ({ Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) })
                        exercise.isFavorite -> ({ Icon(Icons.Filled.Star, null, Modifier.size(FilterChipDefaults.IconSize)) })
                        else -> null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            AssistChip(
                onClick = { actions.onBrowse(!state.browsing) },
                label = { Text(stringResource(if (state.browsing) R.string.log_close_search else R.string.log_browse)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(FilterChipDefaults.IconSize)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        if (state.browsing) {
            val focus = remember { FocusRequester() }
            // Clavier ouvert d'emblée : on vient pour taper un nom.
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = state.query,
                onValueChange = actions.onQuery,
                placeholder = { Text(stringResource(R.string.log_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            PanelCard(Modifier.fillMaxWidth()) {
                Column {
                    if (state.searchResults.isEmpty()) {
                        Text(
                            stringResource(R.string.log_search_empty),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.searchResults.forEachIndexed { index, exercise ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text("${muscleGroupLabel(exercise.muscleGroup)} · ${exerciseTypeLabel(exercise.exerciseType)}")
                            },
                            trailingContent = if (exercise.isFavorite) ({ Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary) }) else null,
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.clickableListItem { actions.onSelectExercise(exercise.id) },
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableListItem(onClick: () -> Unit) = this.then(
    Modifier.heightIn(min = 56.dp).clickable(onClick = onClick),
)

@Composable
private fun SelectedHeader(exercise: Exercise, state: LogSetUiState, zone: ZoneId) {
    Column {
        Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${muscleGroupLabel(exercise.muscleGroup)} · ${exerciseTypeLabel(exercise.exerciseType)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val last = state.lastSet
        if (last != null) {
            Spacer(Modifier.height(8.dp))
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val load = loadLabel(exercise.exerciseType, exercise.loadUnit, last.weight)
                    val effort = effortLabel(exercise.exerciseType, last.repetitions)
                    Text(
                        stringResource(
                            R.string.log_prefilled_from,
                            if (load.isEmpty()) effort else "$load × $effort",
                            "${dayTitle(last.performedAt.toLocalDate(zone), java.time.LocalDate.now(zone))} ${formatTime(last.performedAt, zone)}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputFields(exercise: Exercise, state: LogSetUiState, actions: LogSetActions) {
    val timed = exercise.exerciseType == ExerciseType.TIMED
    if (!timed) {
        val step = formatDecimal(state.increment)
        StepperField(
            label = stringResource(
                if (exercise.exerciseType == ExerciseType.BODYWEIGHT) R.string.log_added_load else R.string.log_load,
            ),
            value = state.weight,
            onValueChange = actions.onWeight,
            onDecrement = { actions.onStepWeight(-1) },
            onIncrement = { actions.onStepWeight(1) },
            decrementLabel = "−$step",
            incrementLabel = "+$step",
            decimal = true,
            suffix = unitLabel(exercise.loadUnit),
            error = inputErrorLabel(state.weightError),
            modifier = Modifier.testTag("weight_field"),
        )
    }
    val stepReps = if (timed) 5 else 1
    StepperField(
        label = stringResource(if (timed) R.string.log_duration else R.string.log_reps),
        value = state.reps,
        onValueChange = actions.onReps,
        onDecrement = { actions.onStepReps(-stepReps) },
        onIncrement = { actions.onStepReps(stepReps) },
        decrementLabel = "−$stepReps",
        incrementLabel = "+$stepReps",
        decimal = false,
        suffix = if (timed) stringResource(R.string.unit_seconds) else null,
        error = inputErrorLabel(state.repsError),
        imeAction = ImeAction.Done,
        modifier = Modifier.testTag("reps_field"),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (if (timed) SECOND_PRESETS else REP_PRESETS).forEach { preset ->
            SuggestionChip(
                onClick = { actions.onReps(preset.toString()) },
                label = { Text(if (timed) "$preset s" else preset.toString(), style = MetricText.value) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun TimeRow(state: LogSetUiState, zone: ZoneId, actions: LogSetActions) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val date = state.performedAt.toLocalDate(zone)
    val today = java.time.LocalDate.now(zone)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MetricLabel(stringResource(R.string.log_when))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { pickDate = true },
                label = { Text(dayTitle(date, today)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(FilterChipDefaults.IconSize)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            AssistChip(
                onClick = { pickTime = true },
                label = { Text(formatTime(state.performedAt, zone), style = MetricText.value) },
                leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(FilterChipDefaults.IconSize)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        if (state.timeInFuture) {
            Text(stringResource(R.string.error_future_time), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (pickDate) {
        DayPickerDialog(
            initial = date,
            maxDate = today,
            onDismiss = { pickDate = false },
            onConfirm = {
                pickDate = false
                actions.onDate(it)
            },
        )
    }
    if (pickTime) {
        TimePickerDialog(
            initial = state.performedAt.toLocalTime(zone),
            onDismiss = { pickTime = false },
            onConfirm = {
                pickTime = false
                actions.onTime(it)
            },
        )
    }
}

@Composable
private fun NotesField(state: LogSetUiState, actions: LogSetActions) {
    if (state.showNotes) {
        OutlinedTextField(
            value = state.notes,
            onValueChange = actions.onNotes,
            label = { Text(stringResource(R.string.log_notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        TextButton(onClick = actions.onShowNotes, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.log_add_note))
        }
    }
}
