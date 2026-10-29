package com.repmomentum.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY isFavorite DESC, name COLLATE NOCASE")
    fun observeActive(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observe(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Exercise?

    @Query("UPDATE exercises SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)
}
