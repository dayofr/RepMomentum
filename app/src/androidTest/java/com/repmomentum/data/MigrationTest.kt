package com.repmomentum.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.RepMomentumDatabase
import com.repmomentum.data.db.SeedData
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

    @Test
    fun migration2To3AddsDescriptionsAndKettlebellExercises() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived) " +
                    "VALUES (1, 'Squat', 'LEGS', 'WEIGHTED', 'KG', 1, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived) " +
                    "VALUES (2, 'Windmill', 'CORE', 'WEIGHTED', 'LB', 0, 1, 0)",
            )
            db.execSQL(
                "INSERT INTO set_entries (id, exerciseId, weight, repetitions, performedAt, notes, createdAt, updatedAt) " +
                    "VALUES (1, 2, 20.0, 5, 1000, NULL, 1000, 1000)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true, RepMomentumDatabase.MIGRATION_2_3)

        fun description(name: String): String? =
            db.query("SELECT description FROM exercises WHERE name = ?", arrayOf(name)).use { c ->
                c.moveToFirst()
                c.getString(0)
            }
        assertThat(description("Squat")).startsWith("Barre sur le haut du dos")
        // L'exercice perso homonyme n'est ni dupliqué ni modifié.
        assertThat(description("Windmill")).isNull()
        db.query("SELECT COUNT(*) FROM exercises WHERE name = 'Windmill'").use { c ->
            c.moveToFirst()
            assertThat(c.getInt(0)).isEqualTo(1)
        }
        assertThat(description("Goblet squat")).isNotEmpty()
        db.query("SELECT COUNT(*) FROM exercises").use { c ->
            c.moveToFirst()
            // Squat + Windmill perso + 11 nouveaux (Windmill déjà présent).
            assertThat(c.getInt(0)).isEqualTo(2 + SeedData.addedInV3.size - 1)
        }
        db.query("SELECT COUNT(*) FROM set_entries").use { c ->
            c.moveToFirst()
            assertThat(c.getInt(0)).isEqualTo(1)
        }
        db.close()
    }

    @Test
    fun migration3To4RemovesPerSideMentionsFromPreloadedOnly() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO exercises (name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived, description) " +
                    "VALUES ('Rowing kettlebell', 'BACK', 'WEIGHTED', 'KG', 0, 0, 0, 'Ancienne consigne. Reps par côté.')",
            )
            db.execSQL(
                "INSERT INTO exercises (name, muscleGroup, exerciseType, loadUnit, isFavorite, isCustom, isArchived, description) " +
                    "VALUES ('Mon row', 'BACK', 'WEIGHTED', 'KG', 0, 1, 0, 'Reps par côté.')",
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true, RepMomentumDatabase.MIGRATION_3_4)

        db.query("SELECT name, description FROM exercises ORDER BY id").use { c ->
            c.moveToNext()
            assertThat(c.getString(1)).doesNotContain("par côté")
            assertThat(c.getString(1)).startsWith("Une main en appui")
            c.moveToNext()
            assertThat(c.getString(1)).isEqualTo("Reps par côté.")
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test"
    }
}
