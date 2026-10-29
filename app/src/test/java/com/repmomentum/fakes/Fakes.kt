package com.repmomentum.fakes

import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.LoggedSet
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.data.db.SetEntry
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.data.repository.RemoveExerciseResult
import com.repmomentum.data.repository.SaveExerciseResult
import com.repmomentum.data.repository.SetDraft
import com.repmomentum.data.repository.SetRepository
import com.repmomentum.data.repository.SettingsRepository
import com.repmomentum.data.repository.UserSettings
import com.repmomentum.domain.dayBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

fun exercise(
    id: Long,
    name: String = "Exo $id",
    type: ExerciseType = ExerciseType.WEIGHTED,
    unit: LoadUnit = if (type == ExerciseType.TIMED) LoadUnit.NONE else LoadUnit.KG,
    favorite: Boolean = false,
) = Exercise(id = id, name = name, muscleGroup = MuscleGroup.CHEST, exerciseType = type, loadUnit = unit, isFavorite = favorite)

fun set(id: Long, exerciseId: Long, weight: Double, reps: Int, at: Long, notes: String? = null) =
    SetEntry(id = id, exerciseId = exerciseId, weight = weight, repetitions = reps, performedAt = at, notes = notes, createdAt = at, updatedAt = at)

class FakeExerciseRepository(initial: List<Exercise>, private val sets: FakeSetRepository) : ExerciseRepository {
    val exercises = MutableStateFlow(initial)

    override fun observeActive(): Flow<List<Exercise>> = exercises.map { list -> list.filter { !it.isArchived } }
    override fun observe(id: Long): Flow<Exercise?> = exercises.map { list -> list.firstOrNull { it.id == id } }
    override fun observeLastUsage(): Flow<Map<Long, Long>> = sets.entries.map { list ->
        list.groupBy { it.exerciseId }.mapValues { (_, s) -> s.maxOf { it.performedAt } }
    }
    override suspend fun get(id: Long) = exercises.value.firstOrNull { it.id == id }
    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        exercises.value = exercises.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
    }
    override suspend fun hasSets(id: Long) = sets.entries.value.any { it.exerciseId == id }
    override suspend fun save(exercise: Exercise): SaveExerciseResult = error("non utilisé")
    override suspend fun remove(id: Long): RemoveExerciseResult = error("non utilisé")
}

class FakeSetRepository(private val clock: Clock) : SetRepository {
    val entries = MutableStateFlow<List<SetEntry>>(emptyList())
    var failWrites = false
    private var nextId = 1000L

    lateinit var exercises: FakeExerciseRepository

    override fun observeDay(date: LocalDate): Flow<List<LoggedSet>> {
        val (start, end) = date.dayBounds(clock.zone)
        return combine(entries, exercises.exercises) { list, ex ->
            list.filter { it.performedAt in start until end }
                .sortedByDescending { it.performedAt }
                .map { s -> LoggedSet(s, ex.first { it.id == s.exerciseId }) }
        }
    }
    override fun observeForExercise(exerciseId: Long) = entries.map { list -> list.filter { it.exerciseId == exerciseId } }
    override suspend fun lastFor(exerciseId: Long) =
        entries.value.filter { it.exerciseId == exerciseId }.maxByOrNull { it.performedAt }
    override suspend fun get(id: Long) = entries.value.firstOrNull { it.id == id }
    override suspend fun add(draft: SetDraft): Long {
        check(!failWrites) { "écriture refusée" }
        val id = nextId++
        entries.value = entries.value + set(id, draft.exerciseId, draft.weight, draft.repetitions, draft.performedAt, draft.notes)
        return id
    }
    override suspend fun update(id: Long, draft: SetDraft) {
        check(!failWrites) { "écriture refusée" }
        entries.value = entries.value.map {
            if (it.id == id) it.copy(exerciseId = draft.exerciseId, weight = draft.weight, repetitions = draft.repetitions, performedAt = draft.performedAt, notes = draft.notes) else it
        }
    }
    override suspend fun delete(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }
    override suspend fun repeat(id: Long): Long {
        val source = get(id)!!
        return add(SetDraft(source.exerciseId, source.weight, source.repetitions, clock.millis(), source.notes))
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state
    override suspend fun setWeightIncrement(value: Double) { state.value = state.value.copy(weightIncrement = value) }
    override suspend fun setDefaultUnit(unit: LoadUnit) { state.value = state.value.copy(defaultUnit = unit) }
}
