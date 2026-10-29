package com.repmomentum.domain

import com.google.common.truth.Truth.assertThat
import com.repmomentum.data.db.ExerciseType.BODYWEIGHT
import com.repmomentum.data.db.ExerciseType.TIMED
import com.repmomentum.data.db.ExerciseType.WEIGHTED
import com.repmomentum.fakes.set
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class SetInputTest {
    private lateinit var previous: Locale

    @Before
    fun setUp() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
    }

    @After
    fun tearDown() = Locale.setDefault(previous)

    @Test
    fun `weighted set needs a positive load`() {
        assertThat(SetInputValidator.validate(WEIGHTED, "62,5", "8")).isEqualTo(SetInputResult.Valid(62.5, 8))
        assertThat(SetInputValidator.validate(WEIGHTED, "62.5", "8")).isEqualTo(SetInputResult.Valid(62.5, 8))
        assertThat(SetInputValidator.validate(WEIGHTED, "", "8")).isEqualTo(SetInputResult.Invalid(InputError.REQUIRED, null))
        assertThat(SetInputValidator.validate(WEIGHTED, "0", "8")).isEqualTo(SetInputResult.Invalid(InputError.TOO_LOW, null))
        assertThat(SetInputValidator.validate(WEIGHTED, "abc", "8")).isEqualTo(SetInputResult.Invalid(InputError.INVALID, null))
        assertThat(SetInputValidator.validate(WEIGHTED, "1001", "8")).isEqualTo(SetInputResult.Invalid(InputError.TOO_HIGH, null))
    }

    @Test
    fun `bodyweight load is optional`() {
        assertThat(SetInputValidator.validate(BODYWEIGHT, "", "10")).isEqualTo(SetInputResult.Valid(0.0, 10))
        assertThat(SetInputValidator.validate(BODYWEIGHT, "0", "10")).isEqualTo(SetInputResult.Valid(0.0, 10))
        assertThat(SetInputValidator.validate(BODYWEIGHT, "10", "6")).isEqualTo(SetInputResult.Valid(10.0, 6))
    }

    @Test
    fun `timed set ignores load and accepts long durations`() {
        assertThat(SetInputValidator.validate(TIMED, "999", "300")).isEqualTo(SetInputResult.Valid(0.0, 300))
        assertThat(SetInputValidator.validate(TIMED, "", "20000")).isEqualTo(SetInputResult.Invalid(null, InputError.TOO_HIGH))
    }

    @Test
    fun `reps must be a positive whole number`() {
        assertThat(SetInputValidator.validate(WEIGHTED, "50", "")).isEqualTo(SetInputResult.Invalid(null, InputError.REQUIRED))
        assertThat(SetInputValidator.validate(WEIGHTED, "50", "0")).isEqualTo(SetInputResult.Invalid(null, InputError.TOO_LOW))
        assertThat(SetInputValidator.validate(WEIGHTED, "50", "8.5")).isEqualTo(SetInputResult.Invalid(null, InputError.INVALID))
        assertThat(SetInputValidator.validate(WEIGHTED, "", "")).isEqualTo(SetInputResult.Invalid(InputError.REQUIRED, InputError.REQUIRED))
    }

    @Test
    fun `parseDecimal accepts comma and dot only`() {
        assertThat(parseDecimal("12,5")).isEqualTo(12.5)
        assertThat(parseDecimal(" 7 ")).isEqualTo(7.0)
        assertThat(parseDecimal(".5")).isEqualTo(0.5)
        assertThat(parseDecimal("")).isNull()
        assertThat(parseDecimal("-3")).isNull()
        assertThat(parseDecimal("1e3")).isNull()
    }

    @Test
    fun `steppers never go below zero and avoid float noise`() {
        assertThat(stepDecimal("60", 2.5)).isEqualTo("62,5")
        assertThat(stepDecimal("1", -2.5)).isEqualTo("0")
        assertThat(stepDecimal("", 1.25)).isEqualTo("1,25")
        assertThat(stepDecimal("0,1", 0.2)).isEqualTo("0,3")
        assertThat(stepInt("8", 1)).isEqualTo("9")
        assertThat(stepInt("0", -1)).isEqualTo("0")
        assertThat(stepInt("", 5)).isEqualTo("5")
    }

    @Test
    fun `prefill uses last set of the exercise`() {
        assertThat(SetPrefill.from(WEIGHTED, set(1, 1, 62.5, 8, 0))).isEqualTo(SetPrefill("62,5", "8"))
        assertThat(SetPrefill.from(BODYWEIGHT, set(1, 1, 0.0, 12, 0))).isEqualTo(SetPrefill("", "12"))
        assertThat(SetPrefill.from(BODYWEIGHT, set(1, 1, 10.0, 6, 0))).isEqualTo(SetPrefill("10", "6"))
        assertThat(SetPrefill.from(TIMED, set(1, 1, 0.0, 45, 0))).isEqualTo(SetPrefill("", "45"))
        assertThat(SetPrefill.from(WEIGHTED, null)).isEqualTo(SetPrefill("", ""))
    }

    @Test
    fun `formats durations and grouped numbers`() {
        assertThat(formatDuration(45)).isEqualTo("45 s")
        assertThat(formatDuration(90)).isEqualTo("1:30")
        assertThat(formatDuration(3725)).isEqualTo("1:02:05")
        assertThat(formatGrouped(3840.0, Locale.US)).isEqualTo("3,840")
    }
}
