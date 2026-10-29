package com.repmomentum.ui.logset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.SetEntry
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.data.repository.SetDraft
import com.repmomentum.data.repository.SetRepository
import com.repmomentum.data.repository.SettingsRepository
import com.repmomentum.data.repository.UserSettings
import com.repmomentum.domain.InputError
import com.repmomentum.domain.SetInputResult
import com.repmomentum.domain.SetInputValidator
import com.repmomentum.domain.SetPrefill
import com.repmomentum.domain.defaultPerformedAt
import com.repmomentum.domain.epochMillisOf
import com.repmomentum.domain.stepDecimal
import com.repmomentum.domain.stepInt
import com.repmomentum.domain.toLocalDate
import com.repmomentum.domain.toLocalTime
import com.repmomentum.ui.LogSetRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/** Série enregistrée, à confirmer par un snackbar. */
data class SetSavedEvent(val setId: Long, val isNew: Boolean)

sealed interface LogSetEvent {
    data class Saved(val event: SetSavedEvent) : LogSetEvent
    data object Deleted : LogSetEvent
}

/** Champs saisis. Conservés dans le [SavedStateHandle] : rotation et mort du processus. */
private data class Form(
    val exerciseId: Long?,
    val weight: String,
    val reps: String,
    val notes: String,
    val performedAt: Long,
    val query: String,
    val browsing: Boolean,
    val showNotes: Boolean,
)

/** Retours de validation et d'enregistrement, éphémères. */
private data class Feedback(
    val weightError: InputError? = null,
    val repsError: InputError? = null,
    val timeInFuture: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

data class LogSetUiState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val notFound: Boolean = false,
    val quickPicks: List<Exercise> = emptyList(),
    val browsing: Boolean = false,
    val query: String = "",
    val searchResults: List<Exercise> = emptyList(),
    val selected: Exercise? = null,
    val lastSet: SetEntry? = null,
    val weight: String = "",
    val reps: String = "",
    val notes: String = "",
    val showNotes: Boolean = false,
    val performedAt: Long = 0,
    val increment: Double = UserSettings.DEFAULT_INCREMENT,
    val weightError: InputError? = null,
    val repsError: InputError? = null,
    val timeInFuture: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val canSave: Boolean get() = selected != null && !isSaving
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogSetViewModel(
    private val route: LogSetRoute,
    private val exercises: ExerciseRepository,
    private val sets: SetRepository,
    settings: SettingsRepository,
    private val clock: Clock,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val zone get() = clock.zone
    private val isEdit = route.setId != null

    private val form = MutableStateFlow(
        Form(
            exerciseId = handle.get<Long>(KEY_EXERCISE),
            weight = handle[KEY_WEIGHT] ?: "",
            reps = handle[KEY_REPS] ?: "",
            notes = handle[KEY_NOTES] ?: "",
            performedAt = handle[KEY_PERFORMED_AT] ?: 0L,
            query = handle[KEY_QUERY] ?: "",
            browsing = handle[KEY_BROWSING] ?: false,
            showNotes = handle[KEY_SHOW_NOTES] ?: false,
        ),
    )
    private val feedback = MutableStateFlow(Feedback())
    private val loading = MutableStateFlow(true)
    private val notFound = MutableStateFlow(false)
    private val events = Channel<LogSetEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LogSetEvent> = events.receiveAsFlow()

    private val exerciseId: Flow<Long?> = form.map { it.exerciseId }.distinctUntilChanged()

    private val lastSet: Flow<SetEntry?> = exerciseId
        .mapLatest { id -> if (id == null || isEdit) null else runCatching { sets.lastFor(id) }.getOrNull() }

    private val catalog: Flow<Catalog> = combine(
        exercises.observeActive(),
        exercises.observeLastUsage(),
    ) { active, usage -> Catalog(active, usage) }
        .catch { emit(Catalog(emptyList(), emptyMap())) }

    private val selectedExercise: Flow<Exercise?> = exerciseId
        .mapLatest { id -> id?.let { runCatching { exercises.get(it) }.getOrNull() } }

    val uiState: StateFlow<LogSetUiState> = combine(
        combine(form, feedback, loading, notFound) { f, fb, l, nf -> Quad(f, fb, l, nf) },
        catalog,
        selectedExercise,
        lastSet,
        settings.settings.catch { emit(UserSettings()) },
    ) { (f, fb, isLoading, missing), cat, selected, last, prefs ->
        LogSetUiState(
            isLoading = isLoading,
            isEdit = isEdit,
            notFound = missing,
            quickPicks = cat.quickPicks(selected),
            browsing = f.browsing || (selected == null && cat.quickPicks(null).isEmpty()),
            query = f.query,
            searchResults = cat.search(f.query),
            selected = selected,
            lastSet = last,
            weight = f.weight,
            reps = f.reps,
            notes = f.notes,
            showNotes = f.showNotes || f.notes.isNotEmpty(),
            performedAt = f.performedAt,
            increment = prefs.weightIncrement,
            weightError = fb.weightError,
            repsError = fb.repsError,
            timeInFuture = fb.timeInFuture,
            isSaving = fb.isSaving,
            saveFailed = fb.saveFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogSetUiState())

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        if (handle.get<Boolean>(KEY_INITIALIZED) == true) {
            loading.value = false
            return
        }
        val setId = route.setId
        if (setId != null) {
            val existing = runCatching { sets.get(setId) }.getOrNull()
            if (existing == null) {
                notFound.value = true
            } else {
                val exercise = exercises.get(existing.exerciseId)
                val type = exercise?.exerciseType ?: ExerciseType.WEIGHTED
                val prefill = SetPrefill.from(type, existing)
                edit {
                    it.copy(
                        exerciseId = existing.exerciseId,
                        weight = prefill.weightText,
                        reps = prefill.repsText,
                        notes = existing.notes.orEmpty(),
                        performedAt = existing.performedAt,
                    )
                }
            }
        } else {
            val day = route.epochDay?.let(LocalDate::ofEpochDay) ?: clock.millis().toLocalDate(zone)
            edit { it.copy(performedAt = defaultPerformedAt(day, clock.millis(), zone)) }
            route.exerciseId?.let { applyExercise(it) }
        }
        handle[KEY_INITIALIZED] = true
        loading.value = false
    }

    fun selectExercise(id: Long) {
        viewModelScope.launch { applyExercise(id) }
    }

    /** Sélectionne l'exercice et, pour une nouvelle série, reprend les valeurs de sa dernière série. */
    private suspend fun applyExercise(id: Long) {
        val exercise = runCatching { exercises.get(id) }.getOrNull() ?: return
        val prefill = if (isEdit) null else SetPrefill.from(exercise.exerciseType, runCatching { sets.lastFor(id) }.getOrNull())
        feedback.update { it.copy(weightError = null, repsError = null) }
        edit {
            it.copy(
                exerciseId = id,
                weight = prefill?.weightText ?: it.weight,
                reps = prefill?.repsText ?: it.reps,
                query = "",
                browsing = false,
            )
        }
    }

    fun setBrowsing(browsing: Boolean) = edit { it.copy(browsing = browsing, query = if (browsing) it.query else "") }

    fun setQuery(query: String) = edit { it.copy(query = query) }

    fun setWeight(text: String) {
        feedback.update { it.copy(weightError = null) }
        edit { it.copy(weight = text.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7)) }
    }

    fun setReps(text: String) {
        feedback.update { it.copy(repsError = null) }
        edit { it.copy(reps = text.filter(Char::isDigit).take(5)) }
    }

    fun stepWeight(direction: Int) = setWeight(stepDecimal(form.value.weight, direction * uiState.value.increment))

    fun stepReps(delta: Int) = setReps(stepInt(form.value.reps, delta))

    fun setNotes(text: String) = edit { it.copy(notes = text.take(MAX_NOTES)) }

    fun showNotes() = edit { it.copy(showNotes = true) }

    fun setDate(date: LocalDate) {
        val time = form.value.performedAt.toLocalTime(zone)
        setPerformedAt(epochMillisOf(date, time, zone))
    }

    fun setTime(time: LocalTime) {
        val date = form.value.performedAt.toLocalDate(zone)
        setPerformedAt(epochMillisOf(date, time, zone))
    }

    private fun setPerformedAt(millis: Long) {
        feedback.update { it.copy(timeInFuture = false) }
        edit { it.copy(performedAt = millis) }
    }

    fun save() {
        val f = form.value
        val exercise = uiState.value.selected ?: return
        if (feedback.value.isSaving) return
        val input = SetInputValidator.validate(exercise.exerciseType, f.weight, f.reps)
        val inFuture = f.performedAt > clock.millis() + FUTURE_TOLERANCE_MS
        if (input is SetInputResult.Invalid || inFuture) {
            feedback.update {
                it.copy(
                    weightError = (input as? SetInputResult.Invalid)?.weightError,
                    repsError = (input as? SetInputResult.Invalid)?.repsError,
                    timeInFuture = inFuture,
                )
            }
            return
        }
        input as SetInputResult.Valid
        val draft = SetDraft(
            exerciseId = exercise.id,
            weight = input.weight,
            repetitions = input.repetitions,
            performedAt = f.performedAt,
            notes = f.notes.trim().ifEmpty { null },
        )
        feedback.update { it.copy(isSaving = true, saveFailed = false) }
        val setId = route.setId
        viewModelScope.launch {
            runCatching {
                if (setId != null) {
                    sets.update(setId, draft)
                    SetSavedEvent(setId, isNew = false)
                } else {
                    SetSavedEvent(sets.add(draft), isNew = true)
                }
            }.onSuccess { events.send(LogSetEvent.Saved(it)) }
                .onFailure { feedback.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    fun delete() {
        val id = route.setId ?: return
        viewModelScope.launch {
            runCatching { sets.delete(id) }
                .onSuccess { events.send(LogSetEvent.Deleted) }
                .onFailure { feedback.update { it.copy(saveFailed = true) } }
        }
    }

    private fun edit(transform: (Form) -> Form) {
        val next = form.updateAndGet(transform)
        handle[KEY_EXERCISE] = next.exerciseId
        handle[KEY_WEIGHT] = next.weight
        handle[KEY_REPS] = next.reps
        handle[KEY_NOTES] = next.notes
        handle[KEY_PERFORMED_AT] = next.performedAt
        handle[KEY_QUERY] = next.query
        handle[KEY_BROWSING] = next.browsing
        handle[KEY_SHOW_NOTES] = next.showNotes
    }

    private fun <T> MutableStateFlow<T>.updateAndGet(transform: (T) -> T): T {
        update(transform)
        return value
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private class Catalog(val active: List<Exercise>, val usage: Map<Long, Long>) {
        /** Favoris puis exercices récents, et l'exercice choisi s'il n'y figure pas. */
        fun quickPicks(selected: Exercise?): List<Exercise> {
            val favorites = active.filter { it.isFavorite }
            val recent = active
                .filter { !it.isFavorite && it.id in usage }
                .sortedByDescending { usage[it.id] }
                .take(MAX_RECENT)
            val picks = favorites + recent
            return if (selected != null && picks.none { it.id == selected.id }) listOf(selected) + picks else picks
        }

        fun search(query: String): List<Exercise> {
            val needle = query.normalized()
            val matches = if (needle.isEmpty()) active else active.filter { needle in it.name.normalized() }
            return matches.sortedWith(compareByDescending<Exercise> { it.isFavorite }.thenByDescending { usage[it.id] ?: 0L })
        }
    }

    private companion object {
        const val KEY_INITIALIZED = "initialized"
        const val KEY_EXERCISE = "exerciseId"
        const val KEY_WEIGHT = "weight"
        const val KEY_REPS = "reps"
        const val KEY_NOTES = "notes"
        const val KEY_PERFORMED_AT = "performedAt"
        const val KEY_QUERY = "query"
        const val KEY_BROWSING = "browsing"
        const val KEY_SHOW_NOTES = "showNotes"
        const val MAX_RECENT = 4
        const val MAX_NOTES = 500
        const val FUTURE_TOLERANCE_MS = 5 * 60_000L
    }
}

/** Minuscules sans accents, pour chercher « developpe » ou « élév ». */
internal fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
