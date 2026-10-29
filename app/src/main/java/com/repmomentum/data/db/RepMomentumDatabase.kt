package com.repmomentum.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Exercise::class, SetEntry::class], version = 3, exportSchema = true)
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
            return builder.addCallback(SeedCallback).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }

        /** v2 : swing et halo. Données seulement, le schéma ne change pas. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = insertMissing(db, SeedData.addedInV2, withDescription = false)
        }

        /** v3 : colonne description, consignes des exercices préchargés, exercices kettlebell. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN description TEXT")
                insertMissing(db, SeedData.addedInV3, withDescription = true)
                // Seuls les exercices préchargés reçoivent leur consigne ; les perso restent intacts.
                SeedData.exercises.forEach { exercise ->
                    db.execSQL(
                        "UPDATE exercises SET description = ? WHERE isCustom = 0 AND description IS NULL AND name = ?",
                        arrayOf<Any?>(exercise.description, exercise.name),
                    )
                }
            }
        }

        /**
         * Insère les exercices absents ; un exercice du même nom (perso compris) est conservé tel quel.
         * [withDescription] faux pour les schémas antérieurs à la v3, sans cette colonne.
         */
        private fun insertMissing(db: SupportSQLiteDatabase, exercises: List<Exercise>, withDescription: Boolean) {
            val columns = "name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived" +
                if (withDescription) ", description" else ""
            val values = "?, ?, ?, ?, ?, 0, 0" + if (withDescription) ", ?" else ""
            exercises.forEach { exercise ->
                val args = buildList<Any?> {
                    add(exercise.name)
                    add(exercise.muscleGroup.name)
                    add(exercise.exerciseType.name)
                    add(exercise.loadUnit.name)
                    add(if (exercise.isFavorite) 1 else 0)
                    if (withDescription) add(exercise.description)
                    add(exercise.name)
                }
                db.execSQL(
                    "INSERT INTO exercises ($columns) SELECT $values " +
                        "WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = ? COLLATE NOCASE)",
                    args.toTypedArray(),
                )
            }
        }
    }

    /** Remplit la bibliothèque dans la transaction de création : elle n'est jamais vue vide. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = insertMissing(db, SeedData.exercises, withDescription = true)
    }
}
