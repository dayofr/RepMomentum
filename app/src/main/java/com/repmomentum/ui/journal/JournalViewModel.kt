package com.repmomentum.ui.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.LoggedSet
import com.repmomentum.data.repository.SetRepository
import com.repmomentum.domain.DailySummary
import com.repmomentum.domain.StatsCalculator
import com.repmomentum.domain.toLocalDate
import com.repmomentum.ui.logset.SetSavedEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class JournalUiState(
    val day: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val sets: List<LoggedSet> = emptyList(),
    val summary: DailySummary = DailySummary.EMPTY,
    val loadFailed: Boolean = false,
) {
    val isToday: Boolean get() = day == today
}

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val sets: SetRepository,
    private val clock: Clock,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val today: LocalDate get() = clock.millis().toLocalDate(clock.zone)

    private val selectedDay: StateFlow<Long> = handle.getStateFlow(KEY_DAY, today.toEpochDay())

    private val events = Channel<SetSavedEvent>(Channel.BUFFERED)

    /** Séries refaites depuis le journal, à confirmer par un snackbar. */
    val repeatedSets: Flow<SetSavedEvent> = events.receiveAsFlow()

    val uiState: StateFlow<JournalUiState> = selectedDay
        .flatMapLatest { epochDay ->
            val day = LocalDate.ofEpochDay(epochDay)
            sets.observeDay(day)
                .map { list ->
                    JournalUiState(
                        day = day,
                        today = today,
                        isLoading = false,
                        sets = list,
                        summary = StatsCalculator.dailySummary(list),
                    )
                }
                .catch { emit(JournalUiState(day = day, today = today, isLoading = false, loadFailed = true)) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            JournalUiState(day = LocalDate.ofEpochDay(selectedDay.value), today = today),
        )

    fun selectDay(day: LocalDate) {
        handle[KEY_DAY] = minOf(day, today).toEpochDay()
    }

    fun previousDay() = selectDay(LocalDate.ofEpochDay(selectedDay.value).minusDays(1))

    fun nextDay() = selectDay(LocalDate.ofEpochDay(selectedDay.value).plusDays(1))

    fun goToToday() = selectDay(today)

    fun repeat(setId: Long) {
        viewModelScope.launch {
            runCatching { sets.repeat(setId) }
                .onSuccess { newId ->
                    // La copie est datée de maintenant : on revient sur aujourd'hui pour la voir.
                    goToToday()
                    events.send(SetSavedEvent(newId, isNew = true))
                }
        }
    }

    private companion object {
        const val KEY_DAY = "day"
    }
}
