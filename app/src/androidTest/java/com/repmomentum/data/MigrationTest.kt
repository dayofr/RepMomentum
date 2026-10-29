package com.repmomentum.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.RepMomentumDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), RepMomentumDatabase::class.java)

    @Test
    fun migration1To2AddsKettlebellExercisesAndKeepsData() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived) " +
                    "VALUES (1, 'Squat', 'LEGS', 'WEIGHTED', 'KG', 1, 0, 0)",
            )
            // Exercice perso déjà créé sous le même nom : il ne doit pas être dupliqué ni modifié.
            db.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived) " +
                    "VALUES (2, 'kettlebell halo', 'CORE', 'WEIGHTED', 'LB', 1, 1, 0)",
            )
            db.execSQL(
                "INSERT INTO set_entries (id, exerciseId, weight, repetitions, performedAt, notes, createdAt, updatedAt) " +
                    "VALUES (1, 1, 100.0, 5, 1000, NULL, 1000, 1000)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, RepMomentumDatabase.MIGRATION_1_2)

        db.query("SELECT name, muscleGroup, loadUnit, isCustom FROM exercises ORDER BY id").use { c ->
            val rows = buildList { while (c.moveToNext()) add("${c.getString(0)}|${c.getString(1)}|${c.getString(2)}|${c.getInt(3)}") }
            assertThat(rows).containsExactly(
                "Squat|LEGS|KG|0",
                "kettlebell halo|CORE|LB|1",
                "Kettlebell swing à deux mains|FULL_BODY|KG|0",
            ).inOrder()
        }
        db.query("SELECT COUNT(*) FROM set_entries").use { c ->
            c.moveToFirst()
            assertThat(c.getInt(0)).isEqualTo(1)
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test"
    }
}
