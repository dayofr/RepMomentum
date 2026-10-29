package com.repmomentum.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.data.db.RepMomentumDatabase
import com.repmomentum.data.db.SeedData
import com.repmomentum.data.repository.RemoveExerciseResult
import com.repmomentum.data.repository.RoomExerciseRepository
import com.repmomentum.data.repository.RoomSetRepository
import com.repmomentum.data.repository.SaveExerciseResult
import com.repmomentum.data.repository.SetDraft
import com.repmomentum.domain.epochMillisOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val today = LocalDate.of(2026, 10, 29)
    private val now = epochMillisOf(today, LocalTime.of(15, 0), zone)
    private lateinit var db: RepMomentumDatabase
    private lateinit var exercises: RoomExerciseRepository
    private lateinit var sets: RoomSetRepository

    @Before
    fun setUp() {
        db = RepMomentumDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
        exercises = RoomExerciseRepository(db.exerciseDao(), db.setEntryDao())
        sets = RoomSetRepository(db.setEntryDao(), Clock.fixed(Instant.ofEpochMilli(now), zone))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun idOf(name: String) = exercises.observeActive().first().first { it.name == name }.id

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = epochMillisOf(date, LocalTime.of(hour, minute), zone)

    @Test
    fun seedIsLoadedWithFavoritesFirst() = runTest {
        val all = exercises.observeActive().first()
        assertThat(all).hasSize(SeedData.exercises.size)
        assertThat(all.takeWhile { it.isFavorite }.map { it.name })
            .containsExactly("Développé couché", "Pompes", "Squat", "Tractions")
    }

    @Test
    fun dayViewKeepsOnlyLocalDayInReverseOrder() = runTest {
        val bench = idOf("Développé couché")
        sets.add(SetDraft(bench, 60.0, 8, at(today, 8), null))
        sets.add(SetDraft(bench, 62.5, 6, at(today, 12), null))
        sets.add(SetDraft(bench, 50.0, 10, at(today.minusDays(1), 23, 59), null))
        sets.add(SetDraft(bench, 50.0, 10, at(today.plusDays(1), 0, 0), null))
        val day = sets.observeDay(today).first()
        assertThat(day.map { it.set.weight }).containsExactly(62.5, 60.0).inOrder()
        assertThat(day.first().exercise.name).isEqualTo("Développé couché")
    }

    @Test
    fun updateAndDeleteAreReflectedImmediately() = runTest {
        val bench = idOf("Développé couché")
        val id = sets.add(SetDraft(bench, 60.0, 8, at(today, 8), null))
        sets.update(id, SetDraft(bench, 65.0, 5, at(today, 9), "lourd"))
        val updated = sets.get(id)!!
        assertThat(updated.weight).isEqualTo(65.0)
        assertThat(updated.notes).isEqualTo("lourd")
        assertThat(updated.updatedAt).isEqualTo(now)
        sets.delete(id)
        assertThat(sets.observeDay(today).first()).isEmpty()
    }

    @Test
    fun repeatCopiesSetAtCurrentTime() = runTest {
        val bench = idOf("Développé couché")
        val id = sets.add(SetDraft(bench, 60.0, 8, at(today.minusDays(2), 8), null))
        val copy = sets.get(sets.repeat(id))!!
        assertThat(copy.performedAt).isEqualTo(now)
        assertThat(copy.weight).isEqualTo(60.0)
        assertThat(sets.lastFor(bench)?.id).isEqualTo(copy.id)
    }

    @Test
    fun exerciseWithSetsCannotBeHardDeleted() = runTest {
        val bench = exercises.get(idOf("Développé couché"))!!
        sets.add(SetDraft(bench.id, 60.0, 8, at(today, 8), null))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { db.exerciseDao().delete(bench) }
        }
    }

    @Test
    fun customExerciseIsArchivedWhenUsedAndDeletedOtherwise() = runTest {
        val unused = (exercises.save(custom("Pull-over")) as SaveExerciseResult.Saved).id
        assertThat(exercises.remove(unused)).isEqualTo(RemoveExerciseResult.DELETED)
        assertThat(exercises.get(unused)).isNull()

        val used = (exercises.save(custom("Hip thrust")) as SaveExerciseResult.Saved).id
        sets.add(SetDraft(used, 80.0, 10, at(today, 8), null))
        assertThat(exercises.remove(used)).isEqualTo(RemoveExerciseResult.ARCHIVED)
        assertThat(exercises.observeActive().first().none { it.id == used }).isTrue()
        // L'historique reste lisible.
        assertThat(sets.observeDay(today).first().single().exercise.isArchived).isTrue()
    }

    @Test
    fun saveRejectsBlankAndDuplicateNames() = runTest {
        assertThat(exercises.save(custom("   "))).isEqualTo(SaveExerciseResult.BlankName)
        assertThat(exercises.save(custom("squat"))).isEqualTo(SaveExerciseResult.DuplicateName)
    }

    @Test
    fun typeAndUnitAreLockedOnceSetsExist() = runTest {
        val id = (exercises.save(custom("Presse")) as SaveExerciseResult.Saved).id
        val saved = exercises.get(id)!!
        assertThat(exercises.save(saved.copy(loadUnit = LoadUnit.LB))).isEqualTo(SaveExerciseResult.Saved(id))
        sets.add(SetDraft(id, 100.0, 10, at(today, 8), null))
        val current = exercises.get(id)!!
        assertThat(exercises.save(current.copy(loadUnit = LoadUnit.KG))).isEqualTo(SaveExerciseResult.LockedByHistory)
        assertThat(exercises.save(current.copy(name = "Presse à cuisses"))).isEqualTo(SaveExerciseResult.Saved(id))
    }

    private fun custom(name: String) = Exercise(
        name = name,
        muscleGroup = MuscleGroup.LEGS,
        exerciseType = ExerciseType.WEIGHTED,
        loadUnit = LoadUnit.KG,
        isCustom = true,
    )
}
