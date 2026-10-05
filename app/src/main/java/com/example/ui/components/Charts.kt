package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryAggregate
import com.example.ui.MonthlyFlowPoint
import com.example.ui.theme.*
import java.util.Locale

// Predefined palette for Donut Slices
val DonutSliceColors = listOf(
    Color(0xFF8B5CF6), // Purple
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF6366F1), // Indigo
    Color(0xFF84CC16), // Lime
    Color(0xFFA855F7)  // Violet
)

@Composable
fun DonutChart(
    title: String,
    aggregates: List<CategoryAggregate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (aggregates.isEmpty()) {
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun dato disponibile",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val totalAmount = aggregates.sumOf { it.amount }

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    aggregates.forEachIndexed { index, agg ->
                        val sweepAngle = (agg.amount / totalAmount * 360f).toFloat()
                        val color = DonutSliceColors[index % DonutSliceColors.size]

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // 2deg gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTALE",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.ITALY, "%.2f €", totalAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                aggregates.take(6).forEachIndexed { index, agg ->
                    val color = DonutSliceColors[index % DonutSliceColors.size]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = agg.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.ITALY, "%.2f € (%.1f%%)", agg.amount, agg.percentage),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlowLineChart(
    flowPoints: List<MonthlyFlowPoint>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Flusso Mensile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(color = IncomeGreen, label = "Entrata")
            LegendItem(color = ExpenseRed, label = "Spesa")
            LegendItem(color = DifferenceBlue, label = "Differenza")
            LegendItem(color = TrendYellow, label = "Trend Anno Prec.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (flowPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun dato di flusso disponibile",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val maxVal = flowPoints.flatMap { listOf(it.income, it.expense, it.difference, it.prevYearTrend) }
                .maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
            val minVal = flowPoints.flatMap { listOf(it.income, it.expense, it.difference, it.prevYearTrend) }
                .minOrNull()?.coerceAtMost(0.0) ?: 0.0

            val range = (maxVal - minVal).coerceAtLeast(1.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val spacing = w / (flowPoints.size - 1).coerceAtLeast(1)

                    fun getY(value: Double): Float {
                        val norm = (value - minVal) / range
                        return h - (norm * h).toFloat()
                    }

                    // Zero Line
                    val zeroY = getY(0.0)
                    drawLine(
                        color = BorderDark,
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw Income Path
                    val pathIncome = Path()
                    val pathExpense = Path()
                    val pathDiff = Path()
                    val pathTrend = Path()

                    flowPoints.forEachIndexed { i, pt ->
                        val x = i * spacing
                        val yInc = getY(pt.income)
                        val yExp = getY(pt.expense)
                        val yDiff = getY(pt.difference)
                        val yTrend = getY(pt.prevYearTrend)

                        if (i == 0) {
                            pathIncome.moveTo(x, yInc)
                            pathExpense.moveTo(x, yExp)
                            pathDiff.moveTo(x, yDiff)
                            pathTrend.moveTo(x, yTrend)
                        } else {
                            pathIncome.lineTo(x, yInc)
                            pathExpense.lineTo(x, yExp)
                            pathDiff.lineTo(x, yDiff)
                            pathTrend.lineTo(x, yTrend)
                        }
                    }

                    drawPath(pathIncome, color = IncomeGreen, style = Stroke(width = 2.5.dp.toPx()))
                    drawPath(pathExpense, color = ExpenseRed, style = Stroke(width = 2.5.dp.toPx()))
                    drawPath(pathDiff, color = DifferenceBlue, style = Stroke(width = 2.dp.toPx()))
                    drawPath(pathTrend, color = TrendYellow, style = Stroke(width = 1.5.dp.toPx()))

                    // Draw Points
                    flowPoints.forEachIndexed { i, pt ->
                        val x = i * spacing
                        drawCircle(IncomeGreen, radius = 3.dp.toPx(), center = Offset(x, getY(pt.income)))
                        drawCircle(ExpenseRed, radius = 3.dp.toPx(), center = Offset(x, getY(pt.expense)))
                        drawCircle(DifferenceBlue, radius = 3.dp.toPx(), center = Offset(x, getY(pt.difference)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                flowPoints.forEach { pt ->
                    Text(
                        text = pt.monthLabel,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SparklineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryLightViolet
) {
    if (values.size < 2) return

    val max = values.maxOrNull() ?: 1.0
    val min = values.minOrNull() ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = w / (values.size - 1)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * spacing
            val norm = (v - min) / range
            val y = h - (norm * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CategoryBarChart(
    title: String,
    aggregates: List<CategoryAggregate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (aggregates.isEmpty()) {
            Text(
                text = "Nessun dato per il grafico a barre",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            val maxAmount = aggregates.maxOf { it.amount }.coerceAtLeast(1.0)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                aggregates.take(8).forEachIndexed { idx, agg ->
                    val color = DonutSliceColors[idx % DonutSliceColors.size]
                    val fraction = (agg.amount / maxAmount).toFloat().coerceIn(0.05f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = agg.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.ITALY, "%.2f € (%.1f%%)", agg.amount, agg.percentage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(CardSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
