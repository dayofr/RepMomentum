package com.repmomentum.domain

import com.repmomentum.data.db.Exercise
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.LoggedSet
import com.repmomentum.data.db.SetEntry
import java.time.LocalDate
import java.time.ZoneId

/** Résumé d'une journée. Les volumes sont séparés par unité : on n'additionne pas kg et lb. */
data class DailySummary(
    val setCount: Int,
    val totalReps: Int,
    val volumeByUnit: Map<LoadUnit, Double>,
    val totalSeconds: Int,
) {
    companion object {
        val EMPTY = DailySummary(0, 0, emptyMap(), 0)
    }
}

/** Grandeur suivie pour un exercice, selon son type et les données disponibles. */
enum class PerformanceMetric {
    /** Charge externe (kg ou lb). */
    LOAD,

    /** Lest ajouté au poids du corps. */
    ADDED_LOAD,

    /** Répétitions (poids du corps sans lest). */
    REPS,

    /** Durée en secondes. */
    DURATION,
}

data class DayPerformance(
    val date: LocalDate,
    /** Meilleure valeur du jour pour la [PerformanceMetric] de l'exercice. */
    val topValue: Double,
    val setCount: Int,
    val totalReps: Int,
    val volume: Double?,
)

data class ExerciseStats(
    val metric: PerformanceMetric,
    val totalSets: Int,
    /** Répétitions cumulées, ou secondes pour un exercice en durée. */
    val totalReps: Int,
    val lastSet: SetEntry?,
    /**
     * Meilleure série enregistrée pour [metric] : plus forte charge (à égalité, plus de reps),
     * ou plus de reps, ou plus longue durée. Ce n'est qu'un maximum des séries saisies,
     * pas un record validé (pas de notion d'échec, d'amplitude ni de 1RM).
     */
    val bestSet: SetEntry?,
    val days: List<DayPerformance>,
)

object StatsCalculator {
    /** Volume d'une série : poids × reps, uniquement pour une charge externe. */
    fun volumeOf(exercise: Exercise, set: SetEntry): Double? =
        if (exercise.exerciseType == ExerciseType.WEIGHTED && exercise.loadUnit != LoadUnit.NONE) {
            set.weight * set.repetitions
        } else {
            null
        }

    fun dailySummary(sets: List<LoggedSet>): DailySummary {
        if (sets.isEmpty()) return DailySummary.EMPTY
        var reps = 0
        var seconds = 0
        val volumes = mutableMapOf<LoadUnit, Double>()
        sets.forEach { (set, exercise) ->
            if (exercise.exerciseType == ExerciseType.TIMED) {
                seconds += set.repetitions
            } else {
                reps += set.repetitions
            }
            volumeOf(exercise, set)?.let { volume ->
                volumes[exercise.loadUnit] = (volumes[exercise.loadUnit] ?: 0.0) + volume
            }
        }
        return DailySummary(
            setCount = sets.size,
            totalReps = reps,
            volumeByUnit = volumes.mapValues { it.value.roundWeight() },
            totalSeconds = seconds,
        )
    }

    fun metricFor(exercise: Exercise, sets: List<SetEntry>): PerformanceMetric = when (exercise.exerciseType) {
        ExerciseType.WEIGHTED -> PerformanceMetric.LOAD
        ExerciseType.TIMED -> PerformanceMetric.DURATION
        ExerciseType.BODYWEIGHT ->
            if (sets.any { it.weight > 0.0 }) PerformanceMetric.ADDED_LOAD else PerformanceMetric.REPS
    }

    /** Statistiques d'un exercice. [sets] dans n'importe quel ordre. */
    fun exerciseStats(exercise: Exercise, sets: List<SetEntry>, zone: ZoneId): ExerciseStats {
        val metric = metricFor(exercise, sets)
        val chronological = sets.sortedWith(compareBy({ it.performedAt }, { it.id }))
        val best = when (metric) {
            PerformanceMetric.LOAD, PerformanceMetric.ADDED_LOAD -> chronological
                .filter { metric == PerformanceMetric.LOAD || it.weight > 0.0 }
                .maxWithOrNull(compareBy<SetEntry>({ it.weight }, { it.repetitions }).thenByDescending { it.performedAt })
            PerformanceMetric.REPS, PerformanceMetric.DURATION -> chronological
                .maxWithOrNull(compareBy<SetEntry> { it.repetitions }.thenByDescending { it.performedAt })
        }
        val days = chronological
            .groupBy { it.performedAt.toLocalDate(zone) }
            .map { (date, daySets) ->
                val volumes = daySets.mapNotNull { volumeOf(exercise, it) }
                DayPerformance(
                    date = date,
                    topValue = daySets.maxOf { topValueOf(metric, it) },
                    setCount = daySets.size,
                    totalReps = daySets.sumOf { it.repetitions },
                    volume = if (volumes.isEmpty()) null else volumes.sum().roundWeight(),
                )
            }
        return ExerciseStats(
            metric = metric,
            totalSets = sets.size,
            totalReps = sets.sumOf { it.repetitions },
            lastSet = chronological.lastOrNull(),
            bestSet = best,
            days = days,
        )
    }

    private fun topValueOf(metric: PerformanceMetric, set: SetEntry): Double = when (metric) {
        PerformanceMetric.LOAD, PerformanceMetric.ADDED_LOAD -> set.weight
        PerformanceMetric.REPS, PerformanceMetric.DURATION -> set.repetitions.toDouble()
    }
}
