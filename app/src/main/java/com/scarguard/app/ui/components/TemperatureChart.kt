package com.scarguard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scarguard.app.ui.theme.RiskAlert
import com.scarguard.app.ui.theme.ScarGuardTheme
import com.scarguard.app.ui.theme.TealPrimary

/** A minimal hand-rolled line chart -- no charting library needed for a single trend line. */
@Composable
fun TemperatureChart(
    points: List<Pair<Long, Float>>, // timestamp millis -> temperature C, chronological order
    baselineTemp: Float?,
    alertTemp: Float?,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text(
                "Readings will appear here once the sensor is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val lineColor = TealPrimary
    val gridColor = MaterialTheme.colorScheme.outline
    val alertColor = RiskAlert

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val temps = points.map { it.second }
        val minTemp = (listOfNotNull(temps.minOrNull(), baselineTemp).minOrNull() ?: 30f) - 0.5f
        val maxTemp = (listOfNotNull(temps.maxOrNull(), alertTemp).maxOrNull() ?: 40f) + 0.5f
        val range = (maxTemp - minTemp).coerceAtLeast(0.5f)

        val leftPad = 4.dp.toPx()
        val rightPad = 4.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = 8.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        fun xFor(index: Int) = leftPad + (chartWidth * index / (points.size - 1).coerceAtLeast(1))
        fun yFor(temp: Float) = topPad + chartHeight * (1f - (temp - minTemp) / range)

        // Reference lines
        baselineTemp?.let { base ->
            drawLine(
                color = gridColor,
                start = Offset(leftPad, yFor(base)),
                end = Offset(size.width - rightPad, yFor(base)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }
        alertTemp?.let { alert ->
            drawLine(
                color = alertColor.copy(alpha = 0.5f),
                start = Offset(leftPad, yFor(alert)),
                end = Offset(size.width - rightPad, yFor(alert)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f))
            )
        }

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, (_, temp) ->
            val x = xFor(index)
            val y = yFor(temp)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        points.forEachIndexed { index, (_, temp) ->
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xFor(index), yFor(temp)))
        }
    }
}

@Preview(showBackground = true, name = "Temperature chart")
@Composable
private fun TemperatureChartPreview() {
    val now = System.currentTimeMillis()
    ScarGuardTheme {
        TemperatureChart(
            points = listOf(
                now - 4 * 3_600_000L to 33.4f,
                now - 3 * 3_600_000L to 33.6f,
                now - 2 * 3_600_000L to 34.1f,
                now - 1 * 3_600_000L to 34.6f,
                now to 34.9f,
            ),
            baselineTemp = 33.5f,
            alertTemp = 35.0f,
        )
    }
}
