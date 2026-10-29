package com.repmomentum.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {
    @Transaction
    @Query(
        "SELECT * FROM set_entries WHERE performedAt >= :start AND performedAt < :end " +
            "ORDER BY performedAt DESC, id DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<LoggedSet>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY performedAt DESC, id DESC")
    fun observeForExercise(exerciseId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY performedAt DESC, id DESC LIMIT 1")
    suspend fun lastForExercise(exerciseId: Long): SetEntry?

    @Query("SELECT exerciseId, MAX(performedAt) AS lastPerformedAt FROM set_entries GROUP BY exerciseId")
    fun observeUsage(): Flow<List<ExerciseUsage>>

    @Query("SELECT COUNT(*) FROM set_entries WHERE exerciseId = :exerciseId")
    suspend fun countForExercise(exerciseId: Long): Int

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun get(id: Long): SetEntry?

    @Insert
    suspend fun insert(set: SetEntry): Long

    @Update
    suspend fun update(set: SetEntry)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
