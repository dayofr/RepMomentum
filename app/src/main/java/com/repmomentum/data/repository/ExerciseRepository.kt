package com.repmomentum.data.repository

import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseDao
import com.repmomentum.data.db.SetEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Résultat de l'enregistrement d'un exercice. */
sealed interface SaveExerciseResult {
    data class Saved(val id: Long) : SaveExerciseResult
    data object BlankName : SaveExerciseResult
    data object DuplicateName : SaveExerciseResult

    /** Type ou unité modifiés alors que des séries existent : l'historique deviendrait incohérent. */
    data object LockedByHistory : SaveExerciseResult
}

/** Issue de la suppression d'un exercice perso. */
enum class RemoveExerciseResult { DELETED, ARCHIVED }

interface ExerciseRepository {
    fun observeActive(): Flow<List<Exercise>>
    fun observe(id: Long): Flow<Exercise?>

    /** Dernière utilisation par exercice (id → epoch millis). */
    fun observeLastUsage(): Flow<Map<Long, Long>>
    suspend fun get(id: Long): Exercise?
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun hasSets(id: Long): Boolean
    suspend fun save(exercise: Exercise): SaveExerciseResult

    /** Supprime un exercice perso sans série, sinon l'archive pour garder l'historique. */
    suspend fun remove(id: Long): RemoveExerciseResult
}

class RoomExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val setDao: SetEntryDao,
) : ExerciseRepository {
    override fun observeActive() = exerciseDao.observeActive()

    override fun observe(id: Long) = exerciseDao.observe(id)

    override fun observeLastUsage(): Flow<Map<Long, Long>> =
        setDao.observeUsage().map { usages -> usages.associate { it.exerciseId to it.lastPerformedAt } }

    override suspend fun get(id: Long) = exerciseDao.get(id)

    override suspend fun setFavorite(id: Long, favorite: Boolean) = exerciseDao.setFavorite(id, favorite)

    override suspend fun hasSets(id: Long) = setDao.countForExercise(id) > 0

    override suspend fun save(exercise: Exercise): SaveExerciseResult {
        val name = exercise.name.trim()
        if (name.isEmpty()) return SaveExerciseResult.BlankName
        val homonym = exerciseDao.findByName(name)
        if (homonym != null && homonym.id != exercise.id) return SaveExerciseResult.DuplicateName
        val cleaned = exercise.copy(name = name)
        if (exercise.id == 0L) return SaveExerciseResult.Saved(exerciseDao.insert(cleaned))

        val previous = exerciseDao.get(exercise.id)
        val definitionChanged = previous != null &&
            (previous.exerciseType != cleaned.exerciseType || previous.loadUnit != cleaned.loadUnit)
        if (definitionChanged && hasSets(exercise.id)) return SaveExerciseResult.LockedByHistory
        exerciseDao.update(cleaned)
        return SaveExerciseResult.Saved(exercise.id)
    }

    override suspend fun remove(id: Long): RemoveExerciseResult {
        val exercise = requireNotNull(exerciseDao.get(id)) { "Exercice $id introuvable" }
        return if (hasSets(id)) {
            exerciseDao.update(exercise.copy(isArchived = true, isFavorite = false))
            RemoveExerciseResult.ARCHIVED
        } else {
            exerciseDao.delete(exercise)
            RemoveExerciseResult.DELETED
        }
    }
}
