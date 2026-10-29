package com.repmomentum.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Arrondi à 2 décimales pour éviter les artefacts binaires (0.1 + 0.2). */
fun Double.roundWeight(): Double = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()

/** Nombre sans zéros inutiles, séparateur décimal de la locale : 12,5 ; 60 ; 1,25. */
fun formatDecimal(value: Double, locale: Locale = Locale.getDefault()): String {
    val format = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale))
    return format.format(value.roundWeight())
}

/** Nombre groupé par milliers : 3 840. */
fun formatGrouped(value: Double, locale: Locale = Locale.getDefault()): String {
    val format = DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(locale))
    return format.format(value)
}

/** Lit un décimal saisi avec virgule ou point. Null si vide ou invalide. */
fun parseDecimal(text: String): Double? {
    val normalized = text.trim().replace(',', '.').replace(" ", "")
    if (normalized.isEmpty()) return null
    if (!normalized.matches(Regex("""\d+(\.\d*)?|\.\d+"""))) return null
    return normalized.toDoubleOrNull()
}

/** Durée en secondes au format court : 45 s, 1:30, 1:02:05. */
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, s)
        m > 0 -> "%d:%02d".format(m, s)
        else -> "$s s"
    }
}
