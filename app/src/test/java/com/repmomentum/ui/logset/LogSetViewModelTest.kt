package com.repmomentum.ui.logset

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.domain.InputError
import com.repmomentum.domain.epochMillisOf
import com.repmomentum.fakes.FakeExerciseRepository
import com.repmomentum.fakes.FakeSetRepository
import com.repmomentum.fakes.FakeSettingsRepository
import com.repmomentum.fakes.exercise
import com.repmomentum.fakes.set
import com.repmomentum.ui.LogSetRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LogSetViewModelTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val now = epochMillisOf(LocalDate.of(2026, 10, 29), LocalTime.of(14, 30), zone)
    private val clock = Clock.fixed(java.time.Instant.ofEpochMilli(now), zone)
    private val bench = exercise(1, "Développé couché", favorite = true)
    private val pullUps = exercise(2, "Tractions", type = ExerciseType.BODYWEIGHT, favorite = true)
    private val curl = exercise(3, "Curl")

    private lateinit var sets: FakeSetRepository
    private lateinit var exercises: FakeExerciseRepository
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
        sets = FakeSetRepository(clock)
        exercises = FakeExerciseRepository(listOf(bench, pullUps, curl), sets)
        sets.exercises = exercises
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(previousLocale)
    }

    private fun TestScope.viewModel(route: LogSetRoute = LogSetRoute(), handle: SavedStateHandle = SavedStateHandle()): LogSetViewModel {
        val vm = LogSetViewModel(route, exercises, sets, FakeSettingsRepository(), clock, handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `new set starts now with favorites as quick picks`() = runTest {
        val vm = viewModel()
        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.performedAt).isEqualTo(now)
        assertThat(state.quickPicks.map { it.id }).containsExactly(1L, 2L)
        assertThat(state.selected).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun `selecting an exercise prefills its last set`() = runTest {
        sets.entries.value = listOf(set(1, 1, 60.0, 8, now - 7_200_000), set(2, 1, 62.5, 6, now - 3_600_000))
        val vm = viewModel()
        vm.selectExercise(1)
        val state = vm.uiState.value
        assertThat(state.selected?.id).isEqualTo(1)
        assertThat(state.weight).isEqualTo("62,5")
        assertThat(state.reps).isEqualTo("6")
        assertThat(state.lastSet?.id).isEqualTo(2)
    }

    @Test
    fun `saving a valid set stores it and emits a new-set event`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 1))
        vm.setWeight("70")
        vm.setReps("5")
        vm.setNotes("  propre  ")
        vm.save()
        val event = vm.eventFlow.first()
        assertThat(event).isInstanceOf(LogSetEvent.Saved::class.java)
        assertThat((event as LogSetEvent.Saved).event.isNew).isTrue()
        val stored = sets.entries.value.single()
        assertThat(stored.weight).isEqualTo(70.0)
        assertThat(stored.repetitions).isEqualTo(5)
        assertThat(stored.performedAt).isEqualTo(now)
        assertThat(stored.notes).isEqualTo("propre")
    }

    @Test
    fun `invalid input shows errors and saves nothing`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 1))
        vm.setWeight("")
        vm.setReps("")
        vm.save()
        assertThat(vm.uiState.value.weightError).isEqualTo(InputError.REQUIRED)
        assertThat(vm.uiState.value.repsError).isEqualTo(InputError.REQUIRED)
        assertThat(sets.entries.value).isEmpty()
    }

    @Test
    fun `bodyweight set saves without load`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 2))
        vm.setReps("10")
        vm.save()
        assertThat(sets.entries.value.single().weight).isEqualTo(0.0)
    }

    @Test
    fun `stepper uses the configured increment`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 1))
        vm.setWeight("60")
        vm.stepWeight(1)
        assertThat(vm.uiState.value.weight).isEqualTo("62,5")
        vm.stepReps(-1)
        assertThat(vm.uiState.value.reps).isEqualTo("0")
    }

    @Test
    fun `future time is refused`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 1))
        vm.setWeight("60")
        vm.setReps("8")
        vm.setTime(LocalTime.of(18, 0))
        vm.save()
        assertThat(vm.uiState.value.timeInFuture).isTrue()
        assertThat(sets.entries.value).isEmpty()
    }

    @Test
    fun `adding from a past day dates the set on that day`() = runTest {
        val vm = viewModel(LogSetRoute(exerciseId = 1, epochDay = LocalDate.of(2026, 10, 20).toEpochDay()))
        val performedAt = vm.uiState.value.performedAt
        assertThat(java.time.Instant.ofEpochMilli(performedAt).atZone(zone).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 20))
    }

    @Test
    fun `editing loads the set and saves an update`() = runTest {
        sets.entries.value = listOf(set(7, 1, 60.0, 8, now - 3_600_000, notes = "lent"))
        val vm = viewModel(LogSetRoute(setId = 7))
        val state = vm.uiState.value
        assertThat(state.isEdit).isTrue()
        assertThat(state.weight).isEqualTo("60")
        assertThat(state.reps).isEqualTo("8")
        assertThat(state.notes).isEqualTo("lent")
        vm.setReps("9")
        vm.save()
        val event = vm.eventFlow.first() as LogSetEvent.Saved
        assertThat(event.event).isEqualTo(SetSavedEvent(7, isNew = false))
        assertThat(sets.entries.value.single().repetitions).isEqualTo(9)
    }

    @Test
    fun `deleting emits an event and removes the set`() = runTest {
        sets.entries.value = listOf(set(7, 1, 60.0, 8, now))
        val vm = viewModel(LogSetRoute(setId = 7))
        vm.delete()
        assertThat(vm.eventFlow.first()).isEqualTo(LogSetEvent.Deleted)
        assertThat(sets.entries.value).isEmpty()
    }

    @Test
    fun `missing set shows not found`() = runTest {
        val vm = viewModel(LogSetRoute(setId = 99))
        assertThat(vm.uiState.value.notFound).isTrue()
    }

    @Test
    fun `persistence failure keeps the form and reports it`() = runTest {
        sets.failWrites = true
        val vm = viewModel(LogSetRoute(exerciseId = 1))
        vm.setWeight("60")
        vm.setReps("8")
        vm.save()
        val state = vm.uiState.value
        assertThat(state.saveFailed).isTrue()
        assertThat(state.isSaving).isFalse()
        assertThat(state.weight).isEqualTo("60")
    }

    @Test
    fun `unsaved input survives recreation from saved state`() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(LogSetRoute(exerciseId = 1), handle)
        first.setWeight("77,5")
        first.setReps("4")
        // Nouveau ViewModel sur le même état sauvegardé (mort du processus).
        sets.entries.value = listOf(set(1, 1, 10.0, 1, now - 1000))
        val second = viewModel(LogSetRoute(exerciseId = 1), handle)
        val state = second.uiState.value
        assertThat(state.selected?.id).isEqualTo(1)
        assertThat(state.weight).isEqualTo("77,5")
        assertThat(state.reps).isEqualTo("4")
    }

    @Test
    fun `search ignores accents and case`() = runTest {
        val vm = viewModel()
        vm.setBrowsing(true)
        vm.setQuery("developpe")
        assertThat(vm.uiState.value.searchResults.map { it.id }).containsExactly(1L)
    }
}
