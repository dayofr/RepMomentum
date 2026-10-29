package com.repmomentum.domain

import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.LoggedSet
import com.repmomentum.fakes.exercise
import com.repmomentum.fakes.set
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val bench = exercise(1, "Développé couché")
    private val benchLb = exercise(2, "Curl", unit = LoadUnit.LB)
    private val pullUps = exercise(3, "Tractions", type = ExerciseType.BODYWEIGHT)
    private val plank = exercise(4, "Gainage", type = ExerciseType.TIMED)

    private fun at(day: Int, hour: Int) =
        LocalDate.of(2026, 10, day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `empty day gives empty summary`() {
        assertThat(StatsCalculator.dailySummary(emptyList())).isEqualTo(DailySummary.EMPTY)
    }

    @Test
    fun `volume counts only external load and never mixes units`() {
        val summary = StatsCalculator.dailySummary(
            listOf(
                LoggedSet(set(1, 1, 60.0, 8, at(28, 8)), bench),
                LoggedSet(set(2, 1, 62.5, 6, at(28, 9)), bench),
                LoggedSet(set(3, 2, 30.0, 10, at(28, 10)), benchLb),
                LoggedSet(set(4, 3, 10.0, 8, at(28, 11)), pullUps),
                LoggedSet(set(5, 4, 0.0, 45, at(28, 12)), plank),
            ),
        )
        assertThat(summary.setCount).isEqualTo(5)
        // Les secondes de gainage ne sont pas des répétitions.
        assertThat(summary.totalReps).isEqualTo(8 + 6 + 10 + 8)
        assertThat(summary.totalSeconds).isEqualTo(45)
        assertThat(summary.volumeByUnit).containsExactly(LoadUnit.KG, 855.0, LoadUnit.LB, 300.0)
    }

    @Test
    fun `bodyweight sets add no volume even with added load`() {
        assertThat(StatsCalculator.volumeOf(pullUps, set(1, 3, 20.0, 5, 0))).isNull()
        assertThat(StatsCalculator.volumeOf(plank, set(1, 4, 0.0, 60, 0))).isNull()
        assertThat(StatsCalculator.volumeOf(bench, set(1, 1, 50.0, 5, 0))).isEqualTo(250.0)
    }

    @Test
    fun `best weighted set is heaviest, then most reps, then first achieved`() {
        val sets = listOf(
            set(1, 1, 80.0, 5, at(20, 8)),
            set(2, 1, 80.0, 6, at(22, 8)),
            set(3, 1, 80.0, 6, at(25, 8)),
            set(4, 1, 70.0, 10, at(26, 8)),
        )
        val stats = StatsCalculator.exerciseStats(bench, sets, zone)
        assertThat(stats.metric).isEqualTo(PerformanceMetric.LOAD)
        assertThat(stats.bestSet?.id).isEqualTo(2)
        assertThat(stats.lastSet?.id).isEqualTo(4)
        assertThat(stats.totalSets).isEqualTo(4)
        assertThat(stats.totalReps).isEqualTo(27)
    }

    @Test
    fun `bodyweight without added load is judged on reps`() {
        val sets = listOf(set(1, 3, 0.0, 8, at(20, 8)), set(2, 3, 0.0, 12, at(21, 8)))
        val stats = StatsCalculator.exerciseStats(pullUps, sets, zone)
        assertThat(stats.metric).isEqualTo(PerformanceMetric.REPS)
        assertThat(stats.bestSet?.id).isEqualTo(2)
    }

    @Test
    fun `bodyweight with ballast is judged on added load, ignoring unweighted sets`() {
        val sets = listOf(set(1, 3, 0.0, 15, at(20, 8)), set(2, 3, 10.0, 6, at(21, 8)), set(3, 3, 5.0, 8, at(22, 8)))
        val stats = StatsCalculator.exerciseStats(pullUps, sets, zone)
        assertThat(stats.metric).isEqualTo(PerformanceMetric.ADDED_LOAD)
        assertThat(stats.bestSet?.id).isEqualTo(2)
    }

    @Test
    fun `days are grouped in local time with daily top value and volume`() {
        // 23h30 à Paris le 27 = 22h30 UTC : doit rester le 27.
        val lateEvening = LocalDate.of(2026, 10, 27).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val sets = listOf(
            set(1, 1, 60.0, 8, lateEvening),
            set(2, 1, 65.0, 5, at(28, 7)),
            set(3, 1, 60.0, 8, at(28, 18)),
        )
        val days = StatsCalculator.exerciseStats(bench, sets, zone).days
        assertThat(days.map { it.date }).containsExactly(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28)).inOrder()
        assertThat(days[1].topValue).isEqualTo(65.0)
        assertThat(days[1].setCount).isEqualTo(2)
        assertThat(days[1].volume).isEqualTo(805.0)
    }

    @Test
    fun `timed exercise is judged on duration and has no volume`() {
        val stats = StatsCalculator.exerciseStats(plank, listOf(set(1, 4, 0.0, 45, at(20, 8)), set(2, 4, 0.0, 90, at(21, 8))), zone)
        assertThat(stats.metric).isEqualTo(PerformanceMetric.DURATION)
        assertThat(stats.bestSet?.id).isEqualTo(2)
        assertThat(stats.days.all { it.volume == null }).isTrue()
    }

    @Test
    fun `no sets gives no best or last`() {
        val stats = StatsCalculator.exerciseStats(bench, emptyList(), zone)
        assertThat(stats.bestSet).isNull()
        assertThat(stats.lastSet).isNull()
        assertThat(stats.days).isEmpty()
    }
}
