package com.repmomentum.data.repository

import com.repmomentum.data.db.LoggedSet
import com.repmomentum.data.db.SetEntry
import com.repmomentum.data.db.SetEntryDao
import com.repmomentum.domain.dayBounds
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate

/** Valeurs saisies pour une série, déjà validées. */
data class SetDraft(
    val exerciseId: Long,
    val weight: Double,
    val repetitions: Int,
    val performedAt: Long,
    val notes: String?,
)

interface SetRepository {
    fun observeDay(date: LocalDate): Flow<List<LoggedSet>>
    fun observeForExercise(exerciseId: Long): Flow<List<SetEntry>>
    suspend fun lastFor(exerciseId: Long): SetEntry?
    suspend fun get(id: Long): SetEntry?
    suspend fun add(draft: SetDraft): Long
    suspend fun update(id: Long, draft: SetDraft)
    suspend fun delete(id: Long)

    /** Refait la même série maintenant. */
    suspend fun repeat(id: Long): Long
}

class RoomSetRepository(
    private val dao: SetEntryDao,
    private val clock: Clock,
) : SetRepository {
    override fun observeDay(date: LocalDate): Flow<List<LoggedSet>> {
        val (start, end) = date.dayBounds(clock.zone)
        return dao.observeBetween(start, end)
    }

    override fun observeForExercise(exerciseId: Long) = dao.observeForExercise(exerciseId)

    override suspend fun lastFor(exerciseId: Long) = dao.lastForExercise(exerciseId)

    override suspend fun get(id: Long) = dao.get(id)

    override suspend fun add(draft: SetDraft): Long {
        val now = clock.millis()
        return dao.insert(
            SetEntry(
                exerciseId = draft.exerciseId,
                weight = draft.weight,
                repetitions = draft.repetitions,
                performedAt = draft.performedAt,
                notes = draft.notes,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun update(id: Long, draft: SetDraft) {
        val existing = requireNotNull(dao.get(id)) { "Série $id introuvable" }
        dao.update(
            existing.copy(
                exerciseId = draft.exerciseId,
                weight = draft.weight,
                repetitions = draft.repetitions,
                performedAt = draft.performedAt,
                notes = draft.notes,
                updatedAt = clock.millis(),
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun repeat(id: Long): Long {
        val source = requireNotNull(dao.get(id)) { "Série $id introuvable" }
        val now = clock.millis()
        return dao.insert(source.copy(id = 0, performedAt = now, createdAt = now, updatedAt = now))
    }
}
