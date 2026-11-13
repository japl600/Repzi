package com.japl.repzi.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

import kotlin.time.Instant

/**
 * Line chart of one metric per session (weight, volume, reps...), drawn with Canvas (no chart library). Points sit at their real dates,
 * so a long pause between two sessions shows as a gap. The record gets a ring and the latest point is filled.
 */
@Composable
internal fun ProgressChart(
    points: List<ChartPoint>,
    wholeNumbers: Boolean,
    formatDate: (Instant) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val scale = remember(points, wholeNumbers) { chartScale(points.minOf { it.value }, points.maxOf { it.value }, wholeNumbers) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val tickLabels = scale.ticks.map { measurer.measure(formatNumber(it), labelStyle) }
        val dateLabels = listOf(points.first(), points.last())
            .map { formatDate(it.date) }
            .distinct()
            .map { measurer.measure(it, labelStyle) }

        val dotRadius = 3.dp.toPx()
        val endRadius = 5.dp.toPx()
        val plotLeft = tickLabels.maxOf { it.size.width } + 10.dp.toPx()
        val plotRight = size.width - 8.dp.toPx()
        val plotTop = 10.dp.toPx()
        val plotBottom = size.height - dateLabels.maxOf { it.size.height } - 10.dp.toPx()

        fun yOf(value: Double) =
            plotBottom - ((value - scale.min) / (scale.max - scale.min)).toFloat() * (plotBottom - plotTop)

        val firstTime = points.first().date.toEpochMilliseconds()
        val span = points.last().date.toEpochMilliseconds() - firstTime
        val xStart = plotLeft + endRadius
        val xEnd = plotRight - endRadius
        fun xOf(point: ChartPoint) =
            if (span == 0L) (xStart + xEnd) / 2
            else xStart + (point.date.toEpochMilliseconds() - firstTime).toFloat() / span * (xEnd - xStart)

        // Grid and value axis.
        scale.ticks.forEachIndexed { index, tick ->
            val y = yOf(tick)
            drawLine(colors.outlineVariant, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
            val label = tickLabels[index]
            drawText(label, topLeft = Offset(plotLeft - 6.dp.toPx() - label.size.width, y - label.size.height / 2f))
        }

        // Date axis: first and last session, or the single one centered.
        val labelY = plotBottom + 6.dp.toPx()
        val first = dateLabels.first()
        if (dateLabels.size == 1) {
            drawText(first, topLeft = Offset((xStart + xEnd) / 2 - first.size.width / 2f, labelY))
        } else {
            val last = dateLabels.last()
            drawText(first, topLeft = Offset(plotLeft, labelY))
            // Skipped when the two labels would touch (a very narrow chart).
            if (first.size.width + last.size.width + 8.dp.toPx() < plotRight - plotLeft) {
                drawText(last, topLeft = Offset(plotRight - last.size.width, labelY))
            }
        }

        val coordinates = points.map { Offset(xOf(it), yOf(it.value)) }
        if (coordinates.size > 1) {
            val line = Path().apply {
                coordinates.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(coordinates.last().x, plotBottom)
                lineTo(coordinates.first().x, plotBottom)
                close()
            }
            drawPath(area, colors.primary.copy(alpha = 0.18f))
            drawPath(
                line,
                colors.primary,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            coordinates.dropLast(1).forEach { drawCircle(colors.tertiary, dotRadius, it) }
        }

        // The record is the last point with the highest value.
        val record = points.maxOf { it.value }
        val recordIndex = points.indexOfLast { it.value == record }
        drawCircle(
            colors.tertiary,
            radius = 8.dp.toPx(),
            center = coordinates[recordIndex],
            style = Stroke(width = 1.5.dp.toPx()),
        )
        // Latest point: filled, with a ring in the card color so it stands out from the line.
        drawCircle(colors.surfaceContainerHighest, endRadius + 2.dp.toPx(), coordinates.last())
        drawCircle(colors.primary, endRadius, coordinates.last())
    }
}

/** Shows whole numbers without the decimal part: 60 instead of 60.0. */
internal fun formatNumber(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()
