package com.repmomentum.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repmomentum.domain.DayPerformance
import com.repmomentum.ui.theme.MetricText
import java.time.format.DateTimeFormatter

/**
 * Courbe de la meilleure valeur par jour. Axe X proportionnel au temps, pour ne pas
 * rapprocher artificiellement deux séances éloignées.
 */
@Composable
fun ProgressChart(
    days: List<DayPerformance>,
    formatValue: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val minValue = days.minOf { it.topValue }
    val maxValue = days.maxOf { it.topValue }
    val firstDay = days.first().date.toEpochDay()
    val lastDay = days.last().date.toEpochDay()
    val dateFormat = DateTimeFormatter.ofPattern("d MMM")
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row {
            Text(formatValue(maxValue), style = MetricText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val pad = 8.dp.toPx()
            val w = size.width - 2 * pad
            val h = size.height - 2 * pad
            val span = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
            val daySpan = (lastDay - firstDay).takeIf { it > 0 } ?: 1L
            fun point(d: DayPerformance): Offset {
                val x = pad + w * (d.date.toEpochDay() - firstDay).toFloat() / daySpan
                val y = if (maxValue == minValue) pad + h / 2 else pad + h * (1f - ((d.topValue - minValue) / span).toFloat())
                return Offset(x, y)
            }
            listOf(0f, 0.5f, 1f).forEach { f ->
                val y = pad + h * f
                drawLine(grid, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }
            val path = Path()
            days.forEachIndexed { index, d ->
                val p = point(d)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            days.forEach { drawCircle(line, radius = 4.dp.toPx(), center = point(it)) }
        }
        Row {
            Text(formatValue(minValue), style = MetricText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(days.first().date.format(dateFormat), style = MetricText.label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(days.last().date.format(dateFormat), style = MetricText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
