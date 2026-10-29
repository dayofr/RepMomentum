package com.repmomentum.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.ui.logset.normalized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtre de la bibliothèque : tout, favoris, ou un groupe musculaire. */
sealed interface ExerciseFilter {
    data object All : ExerciseFilter
    data object Favorites : ExerciseFilter
    data class Group(val group: MuscleGroup) : ExerciseFilter

    fun key(): String = when (this) {
        All -> "all"
        Favorites -> "fav"
        is Group -> group.name
    }

    companion object {
        fun fromKey(key: String?): ExerciseFilter = when (key) {
            null, "all" -> All
            "fav" -> Favorites
            else -> MuscleGroup.entries.firstOrNull { it.name == key }?.let(::Group) ?: All
        }
    }
}

data class ExerciseItem(val exercise: Exercise, val lastUsed: Long?)

data class ExercisesUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val filter: ExerciseFilter = ExerciseFilter.All,
    val favorites: List<ExerciseItem> = emptyList(),
    val others: List<ExerciseItem> = emptyList(),
) {
    val isEmpty: Boolean get() = favorites.isEmpty() && others.isEmpty()
}

class ExercisesViewModel(
    private val exercises: ExerciseRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val query = MutableStateFlow(handle[KEY_QUERY] ?: "")
    private val filter = MutableStateFlow(ExerciseFilter.fromKey(handle[KEY_FILTER]))

    val uiState: StateFlow<ExercisesUiState> = combine(
        exercises.observeActive(),
        exercises.observeLastUsage(),
        query,
        filter,
    ) { all, usage, q, f ->
        val needle = q.normalized()
        val visible = all
            .filter { needle.isEmpty() || needle in it.name.normalized() }
            .filter {
                when (f) {
                    ExerciseFilter.All -> true
                    ExerciseFilter.Favorites -> it.isFavorite
                    is ExerciseFilter.Group -> it.muscleGroup == f.group
                }
            }
            .map { ExerciseItem(it, usage[it.id]) }
        val (favorites, others) = visible.partition { it.exercise.isFavorite }
        ExercisesUiState(isLoading = false, query = q, filter = f, favorites = favorites, others = others)
    }
        .catch { emit(ExercisesUiState(isLoading = false, loadFailed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    fun setQuery(value: String) {
        query.value = value
        handle[KEY_QUERY] = value
    }

    fun setFilter(value: ExerciseFilter) {
        filter.value = value
        handle[KEY_FILTER] = value.key()
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch { runCatching { exercises.setFavorite(exercise.id, !exercise.isFavorite) } }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
    }
}
