package com.repmomentum.ui.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.SetEntry
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.data.repository.SetRepository
import com.repmomentum.domain.ExerciseStats
import com.repmomentum.domain.StatsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class ProgressUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** Exercices ayant au moins une série, du plus récent au plus ancien. */
    val candidates: List<Exercise> = emptyList(),
    val exercise: Exercise? = null,
    val stats: ExerciseStats? = null,
    /** Séries de l'exercice, plus récentes en premier. */
    val history: List<SetEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    initialExerciseId: Long?,
    exercises: ExerciseRepository,
    private val sets: SetRepository,
    private val clock: Clock,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val chosenId: StateFlow<Long?> = handle.getStateFlow(KEY_EXERCISE, initialExerciseId)

    private val candidates: Flow<List<Exercise>> = combine(
        exercises.observeActive(),
        exercises.observeLastUsage(),
    ) { active, usage -> active.filter { it.id in usage }.sortedByDescending { usage[it.id] } }

    /** Exercice affiché : celui choisi, sinon le dernier utilisé. */
    private val effectiveId: Flow<Long?> = combine(chosenId, candidates) { chosen, list -> chosen ?: list.firstOrNull()?.id }

    val uiState: StateFlow<ProgressUiState> = combine(candidates, effectiveId) { list, id -> list to id }
        .flatMapLatest { (list, id) ->
            if (id == null) {
                flowOf(ProgressUiState(isLoading = false, candidates = list))
            } else {
                combine(exercises.observe(id), sets.observeForExercise(id)) { exercise, history ->
                    ProgressUiState(
                        isLoading = false,
                        candidates = list,
                        exercise = exercise,
                        stats = exercise?.let { StatsCalculator.exerciseStats(it, history, clock.zone) },
                        history = history,
                    )
                }
            }
        }
        .catch { emit(ProgressUiState(isLoading = false, loadFailed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectExercise(id: Long) {
        handle[KEY_EXERCISE] = id
    }

    private companion object {
        const val KEY_EXERCISE = "exerciseId"
    }
}
