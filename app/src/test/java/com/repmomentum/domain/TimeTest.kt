package com.repmomentum.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeTest {
    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun `day bounds follow local midnight, including DST change`() {
        // 25 octobre 2026 : passage à l'heure d'hiver, jour de 25 heures.
        val (start, end) = LocalDate.of(2026, 10, 25).dayBounds(paris)
        assertThat(end - start).isEqualTo(25 * 3_600_000L)
        assertThat(start.toLocalDate(paris)).isEqualTo(LocalDate.of(2026, 10, 25))
        assertThat((end - 1).toLocalDate(paris)).isEqualTo(LocalDate.of(2026, 10, 25))
        assertThat(end.toLocalDate(paris)).isEqualTo(LocalDate.of(2026, 10, 26))
    }

    @Test
    fun `new set defaults to now today, or same clock time on a past day`() {
        val now = epochMillisOf(LocalDate.of(2026, 10, 29), LocalTime.of(14, 32, 17), paris)
        assertThat(defaultPerformedAt(LocalDate.of(2026, 10, 29), now, paris)).isEqualTo(now)
        val past = defaultPerformedAt(LocalDate.of(2026, 10, 20), now, paris)
        assertThat(past.toLocalDate(paris)).isEqualTo(LocalDate.of(2026, 10, 20))
        assertThat(past.toLocalTime(paris)).isEqualTo(LocalTime.of(14, 32))
    }
}
