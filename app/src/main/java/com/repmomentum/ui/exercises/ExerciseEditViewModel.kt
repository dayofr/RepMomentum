package com.repmomentum.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.data.repository.RemoveExerciseResult
import com.repmomentum.data.repository.SaveExerciseResult
import com.repmomentum.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExerciseFormError { BLANK_NAME, DUPLICATE_NAME, LOCKED_BY_HISTORY, SAVE_FAILED }

data class ExerciseEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.FULL_BODY,
    val exerciseType: ExerciseType = ExerciseType.WEIGHTED,
    val loadUnit: LoadUnit = LoadUnit.KG,
    val isFavorite: Boolean = false,
    /** Des séries existent : type et unité ne peuvent plus changer. */
    val hasHistory: Boolean = false,
    val error: ExerciseFormError? = null,
    val isSaving: Boolean = false,
)

sealed interface ExerciseEditEvent {
    data object Saved : ExerciseEditEvent
    data class Removed(val result: RemoveExerciseResult) : ExerciseEditEvent
}

class ExerciseEditViewModel(
    private val exerciseId: Long?,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseEditUiState())
    val uiState: StateFlow<ExerciseEditUiState> = _state.asStateFlow()
    private val events = Channel<ExerciseEditEvent>(Channel.BUFFERED)
    val eventFlow: Flow<ExerciseEditEvent> = events.receiveAsFlow()
    private var original: Exercise? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val existing = exerciseId?.let { runCatching { exercises.get(it) }.getOrNull() }
        original = existing
        val base = if (existing != null) {
            ExerciseEditUiState(
                isLoading = false,
                isNew = false,
                name = existing.name,
                muscleGroup = existing.muscleGroup,
                exerciseType = existing.exerciseType,
                loadUnit = existing.loadUnit,
                isFavorite = existing.isFavorite,
                hasHistory = runCatching { exercises.hasSets(existing.id) }.getOrDefault(true),
            )
        } else {
            val unit = runCatching { settings.settings.first().defaultUnit }.getOrDefault(LoadUnit.KG)
            ExerciseEditUiState(isLoading = false, loadUnit = unit)
        }
        // Saisie en cours restaurée après la mort du processus.
        _state.value = base.copy(
            name = handle[KEY_NAME] ?: base.name,
            muscleGroup = handle.get<String>(KEY_GROUP)?.let(MuscleGroup::valueOf) ?: base.muscleGroup,
            exerciseType = handle.get<String>(KEY_TYPE)?.let(ExerciseType::valueOf) ?: base.exerciseType,
            loadUnit = handle.get<String>(KEY_UNIT)?.let(LoadUnit::valueOf) ?: base.loadUnit,
            isFavorite = handle[KEY_FAVORITE] ?: base.isFavorite,
        )
    }

    fun setName(value: String) = edit { it.copy(name = value.take(MAX_NAME), error = null) }

    fun setGroup(value: MuscleGroup) = edit { it.copy(muscleGroup = value) }

    fun setType(value: ExerciseType) = edit { state ->
        if (state.hasHistory) return@edit state
        val unit = when {
            value == ExerciseType.TIMED -> LoadUnit.NONE
            state.loadUnit == LoadUnit.NONE -> LoadUnit.KG
            else -> state.loadUnit
        }
        state.copy(exerciseType = value, loadUnit = unit)
    }

    fun setUnit(value: LoadUnit) = edit { if (it.hasHistory || it.exerciseType == ExerciseType.TIMED) it else it.copy(loadUnit = value) }

    fun setFavorite(value: Boolean) = edit { it.copy(isFavorite = value) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val exercise = (original ?: Exercise(name = "", muscleGroup = s.muscleGroup, exerciseType = s.exerciseType, loadUnit = s.loadUnit, isCustom = true))
                .copy(name = s.name, muscleGroup = s.muscleGroup, exerciseType = s.exerciseType, loadUnit = s.loadUnit, isFavorite = s.isFavorite)
            val result = runCatching { exercises.save(exercise) }.getOrNull()
            val error = when (result) {
                is SaveExerciseResult.Saved -> null
                SaveExerciseResult.BlankName -> ExerciseFormError.BLANK_NAME
                SaveExerciseResult.DuplicateName -> ExerciseFormError.DUPLICATE_NAME
                SaveExerciseResult.LockedByHistory -> ExerciseFormError.LOCKED_BY_HISTORY
                null -> ExerciseFormError.SAVE_FAILED
            }
            _state.update { it.copy(isSaving = false, error = error) }
            if (error == null) events.send(ExerciseEditEvent.Saved)
        }
    }

    fun remove() {
        val id = original?.id ?: return
        viewModelScope.launch {
            runCatching { exercises.remove(id) }
                .onSuccess { events.send(ExerciseEditEvent.Removed(it)) }
                .onFailure { _state.update { it.copy(error = ExerciseFormError.SAVE_FAILED) } }
        }
    }

    private fun edit(transform: (ExerciseEditUiState) -> ExerciseEditUiState) {
        _state.update(transform)
        val s = _state.value
        handle[KEY_NAME] = s.name
        handle[KEY_GROUP] = s.muscleGroup.name
        handle[KEY_TYPE] = s.exerciseType.name
        handle[KEY_UNIT] = s.loadUnit.name
        handle[KEY_FAVORITE] = s.isFavorite
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_GROUP = "group"
        const val KEY_TYPE = "type"
        const val KEY_UNIT = "unit"
        const val KEY_FAVORITE = "favorite"
        const val MAX_NAME = 60
    }
}
