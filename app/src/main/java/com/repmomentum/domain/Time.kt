package com.repmomentum.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Bornes [début, fin) d'un jour local, en epoch millis. Gère les jours de 23 ou 25 h. */
fun LocalDate.dayBounds(zone: ZoneId): Pair<Long, Long> {
    val start = atStartOfDay(zone).toInstant().toEpochMilli()
    val end = plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.toLocalTime(zone: ZoneId): LocalTime = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

/** Combine un jour et une heure locaux en epoch millis. */
fun epochMillisOf(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
    date.atTime(time).atZone(zone).toInstant().toEpochMilli()

/**
 * Instant proposé pour une nouvelle série affichée sur [date] : maintenant si c'est aujourd'hui,
 * sinon ce jour-là à l'heure actuelle (saisie a posteriori).
 */
fun defaultPerformedAt(date: LocalDate, nowMillis: Long, zone: ZoneId): Long {
    if (nowMillis.toLocalDate(zone) == date) return nowMillis
    val time = nowMillis.toLocalTime(zone).withSecond(0).withNano(0)
    return epochMillisOf(date, time, zone)
}
