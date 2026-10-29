package com.repmomentum.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Exercise::class, SetEntry::class], version = 2, exportSchema = true)
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
            return builder.addCallback(SeedCallback).addMigrations(MIGRATION_1_2).build()
        }

        /** v2 : exercices kettlebell. Données seulement, le schéma ne change pas. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = insertMissing(db, SeedData.addedInV2)
        }

        /** Insère les exercices absents ; un exercice perso du même nom est conservé tel quel. */
        private fun insertMissing(db: SupportSQLiteDatabase, exercises: List<Exercise>) {
            exercises.forEach { exercise ->
                db.execSQL(
                    """
                    INSERT INTO exercises (name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived)
                    SELECT ?, ?, ?, ?, ?, 0, 0
                    WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = ? COLLATE NOCASE)
                    """.trimIndent(),
                    arrayOf<Any>(
                        exercise.name,
                        exercise.muscleGroup.name,
                        exercise.exerciseType.name,
                        exercise.loadUnit.name,
                        if (exercise.isFavorite) 1 else 0,
                        exercise.name,
                    ),
                )
            }
        }
    }

    /** Remplit la bibliothèque dans la transaction de création : elle n'est jamais vue vide. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = insertMissing(db, SeedData.exercises)
    }
}
