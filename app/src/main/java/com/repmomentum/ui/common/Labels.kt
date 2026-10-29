package com.repmomentum.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.repmomentum.R
import com.repmomentum.data.db.ExerciseType
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.db.MuscleGroup
import com.repmomentum.domain.InputError
import com.repmomentum.domain.formatDecimal
import com.repmomentum.domain.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun unitLabel(unit: LoadUnit): String = when (unit) {
    LoadUnit.KG -> stringResource(R.string.unit_kg)
    LoadUnit.LB -> stringResource(R.string.unit_lb)
    LoadUnit.NONE -> ""
}

@Composable
fun muscleGroupLabel(group: MuscleGroup): String = stringResource(
    when (group) {
        MuscleGroup.CHEST -> R.string.group_chest
        MuscleGroup.BACK -> R.string.group_back
        MuscleGroup.SHOULDERS -> R.string.group_shoulders
        MuscleGroup.BICEPS -> R.string.group_biceps
        MuscleGroup.TRICEPS -> R.string.group_triceps
        MuscleGroup.LEGS -> R.string.group_legs
        MuscleGroup.CORE -> R.string.group_core
        MuscleGroup.FULL_BODY -> R.string.group_full_body
    },
)

@Composable
fun exerciseTypeLabel(type: ExerciseType): String = stringResource(
    when (type) {
        ExerciseType.WEIGHTED -> R.string.type_weighted
        ExerciseType.BODYWEIGHT -> R.string.type_bodyweight
        ExerciseType.TIMED -> R.string.type_timed
    },
)

/** Charge lisible : « 60 kg », « PDC + 10 kg », « PDC », ou vide pour une durée. */
@Composable
fun loadLabel(type: ExerciseType, unit: LoadUnit, weight: Double): String = when (type) {
    ExerciseType.WEIGHTED -> "${formatDecimal(weight)} ${unitLabel(unit)}"
    ExerciseType.BODYWEIGHT ->
        if (weight > 0.0) {
            stringResource(R.string.load_bodyweight_plus, "${formatDecimal(weight)} ${unitLabel(unit)}")
        } else {
            stringResource(R.string.load_bodyweight)
        }
    ExerciseType.TIMED -> ""
}

/** Effort lisible : « 8 reps » ou « 45 s ». */
@Composable
fun effortLabel(type: ExerciseType, repetitions: Int): String =
    if (type == ExerciseType.TIMED) {
        formatDuration(repetitions)
    } else {
        androidx.compose.ui.res.pluralStringResource(R.plurals.reps, repetitions, repetitions)
    }

@Composable
fun inputErrorLabel(error: InputError?): String? = error?.let {
    stringResource(
        when (it) {
            InputError.REQUIRED -> R.string.error_required
            InputError.INVALID -> R.string.error_invalid
            InputError.TOO_LOW -> R.string.error_too_low
            InputError.TOO_HIGH -> R.string.error_too_high
        },
    )
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatTime(epochMillis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(timeFormatter)

fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** « Aujourd'hui », « Hier », ou « lundi 27 octobre ». */
@Composable
fun dayTitle(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEEE d MMMM" else "EEEE d MMMM yyyy"))
        .replaceFirstChar { it.uppercase() }
}
