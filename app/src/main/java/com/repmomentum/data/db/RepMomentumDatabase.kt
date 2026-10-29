package com.repmomentum.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Exercise::class, SetEntry::class], version = 1, exportSchema = true)
abstract class RepMomentumDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        private const val NAME = "repmomentum.db"

        fun create(context: Context, inMemory: Boolean = false): RepMomentumDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, RepMomentumDatabase::class.java)
            } else {
                Room.databaseBuilder(context, RepMomentumDatabase::class.java, NAME)
            }
            return builder.addCallback(SeedCallback).build()
        }
    }

    /** Remplit la bibliothèque dans la transaction de création : elle n'est jamais vue vide. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            SeedData.exercises.forEach { exercise ->
                val values = ContentValues().apply {
                    put("name", exercise.name)
                    put("muscleGroup", exercise.muscleGroup.name)
                    put("exerciseType", exercise.exerciseType.name)
                    put("loadUnit", exercise.loadUnit.name)
                    put("isFavorite", exercise.isFavorite)
                    put("isCustom", false)
                    put("isArchived", false)
                }
                db.insert("exercises", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }
}
