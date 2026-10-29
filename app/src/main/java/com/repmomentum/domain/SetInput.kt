package com.repmomentum.domain

import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.SetEntry

enum class InputError { REQUIRED, INVALID, TOO_LOW, TOO_HIGH }

sealed interface SetInputResult {
    data class Valid(val weight: Double, val repetitions: Int) : SetInputResult
    data class Invalid(val weightError: InputError?, val repsError: InputError?) : SetInputResult
}

/** Validation de la saisie d'une série selon le type d'exercice. */
object SetInputValidator {
    const val MAX_WEIGHT = 1000.0
    const val MAX_REPS = 1000
    const val MAX_SECONDS = 4 * 3600

    fun validate(type: ExerciseType, weightText: String, repsText: String): SetInputResult {
        val (weight, weightError) = validateWeight(type, weightText)
        val (reps, repsError) = validateReps(type, repsText)
        return if (weightError == null && repsError == null) {
            SetInputResult.Valid(weight!!, reps!!)
        } else {
            SetInputResult.Invalid(weightError, repsError)
        }
    }

    private fun validateWeight(type: ExerciseType, text: String): Pair<Double?, InputError?> {
        if (type == ExerciseType.TIMED) return 0.0 to null
        if (text.isBlank()) {
            // Sans lest, le poids du corps se note 0.
            return if (type == ExerciseType.BODYWEIGHT) 0.0 to null else null to InputError.REQUIRED
        }
        val value = parseDecimal(text) ?: return null to InputError.INVALID
        return when {
            type == ExerciseType.WEIGHTED && value <= 0.0 -> null to InputError.TOO_LOW
            value > MAX_WEIGHT -> null to InputError.TOO_HIGH
            else -> value.roundWeight() to null
        }
    }

    private fun validateReps(type: ExerciseType, text: String): Pair<Int?, InputError?> {
        if (text.isBlank()) return null to InputError.REQUIRED
        val value = text.trim().toIntOrNull() ?: return null to InputError.INVALID
        val max = if (type == ExerciseType.TIMED) MAX_SECONDS else MAX_REPS
        return when {
            value < 1 -> null to InputError.TOO_LOW
            value > max -> null to InputError.TOO_HIGH
            else -> value to null
        }
    }
}

/** Valeurs proposées à l'ouverture de la saisie : celles de la dernière série de l'exercice. */
data class SetPrefill(val weightText: String, val repsText: String) {
    companion object {
        fun from(type: ExerciseType, last: SetEntry?): SetPrefill {
            if (last == null) return SetPrefill(weightText = "", repsText = "")
            val weight = when {
                type == ExerciseType.TIMED -> ""
                type == ExerciseType.BODYWEIGHT && last.weight == 0.0 -> ""
                else -> formatDecimal(last.weight)
            }
            return SetPrefill(weightText = weight, repsText = last.repetitions.toString())
        }
    }
}

/** Incrémente un champ numérique saisi, sans descendre sous zéro. */
fun stepDecimal(text: String, delta: Double): String {
    val next = ((parseDecimal(text) ?: 0.0) + delta).coerceAtLeast(0.0).roundWeight()
    return formatDecimal(next)
}

fun stepInt(text: String, delta: Int): String {
    val next = ((text.trim().toIntOrNull() ?: 0) + delta).coerceAtLeast(0)
    return next.toString()
}
