package com.repmomentum.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.R
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.data.repository.RemoveExerciseResult
import com.repmomentum.ui.common.LoadingState
import com.repmomentum.ui.common.MetricLabel
import com.repmomentum.ui.common.appViewModelFactory
import com.repmomentum.ui.common.exerciseTypeLabel
import com.repmomentum.ui.common.muscleGroupLabel
import com.repmomentum.ui.common.unitLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(
    exerciseId: Long?,
    onClose: () -> Unit,
    onRemoved: (messageRes: Int) -> Unit,
    viewModel: ExerciseEditViewModel = viewModel(
        factory = appViewModelFactory { c, handle ->
            ExerciseEditViewModel(exerciseId, c.exerciseRepository, c.settingsRepository, handle)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ExerciseEditEvent.Saved -> onClose()
                is ExerciseEditEvent.Removed -> onRemoved(
                    if (event.result == RemoveExerciseResult.ARCHIVED) R.string.snackbar_exercise_archived else R.string.snackbar_exercise_deleted,
                )
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.exercise_new_title else R.string.exercise_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
                },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium) }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val nameError = when (state.error) {
                ExerciseFormError.BLANK_NAME -> stringResource(R.string.error_required)
                ExerciseFormError.DUPLICATE_NAME -> stringResource(R.string.error_duplicate_name)
                else -> null
            }
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.exercise_name)) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text(stringResource(R.string.exercise_description)) },
                placeholder = { Text(stringResource(R.string.exercise_description_hint)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricLabel(stringResource(R.string.exercise_group))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MuscleGroup.entries.forEach { group ->
                        FilterChip(
                            selected = state.muscleGroup == group,
                            onClick = { viewModel.setGroup(group) },
                            label = { Text(muscleGroupLabel(group)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricLabel(stringResource(R.string.exercise_type))
                val types = ExerciseType.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.exerciseType == type,
                            onClick = { viewModel.setType(type) },
                            enabled = !state.hasHistory,
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            label = { Text(exerciseTypeLabel(type)) },
                        )
                    }
                }
                Text(
                    stringResource(
                        when (state.exerciseType) {
                            ExerciseType.WEIGHTED -> R.string.exercise_type_weighted_help
                            ExerciseType.BODYWEIGHT -> R.string.exercise_type_bodyweight_help
                            ExerciseType.TIMED -> R.string.exercise_type_timed_help
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.exerciseType != ExerciseType.TIMED) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricLabel(stringResource(R.string.exercise_unit))
                    val units = listOf(LoadUnit.KG, LoadUnit.LB)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        units.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = state.loadUnit == unit,
                                onClick = { viewModel.setUnit(unit) },
                                enabled = !state.hasHistory,
                                shape = SegmentedButtonDefaults.itemShape(index, units.size),
                                label = { Text(unitLabel(unit)) },
                            )
                        }
                    }
                }
            }
            if (state.hasHistory) {
                Text(
                    stringResource(R.string.exercise_locked_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (state.error == ExerciseFormError.LOCKED_BY_HISTORY || state.error == ExerciseFormError.SAVE_FAILED) {
                Text(
                    stringResource(if (state.error == ExerciseFormError.SAVE_FAILED) R.string.error_save else R.string.exercise_locked_help),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.exercise_favorite), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isFavorite, onCheckedChange = viewModel::setFavorite)
            }
            if (!state.isNew) {
                OutlinedButton(
                    onClick = { confirmRemove = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(if (state.hasHistory) R.string.action_archive_exercise else R.string.action_delete_exercise),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(if (state.hasHistory) R.string.archive_exercise_title else R.string.delete_exercise_title)) },
            text = { Text(stringResource(if (state.hasHistory) R.string.archive_exercise_message else R.string.delete_exercise_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.remove()
                }) {
                    Text(
                        stringResource(if (state.hasHistory) R.string.action_archive else R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
