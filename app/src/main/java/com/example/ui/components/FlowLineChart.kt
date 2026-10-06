package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MonthlyFlowPoint
import com.example.ui.theme.*
import java.util.*

@Composable
fun FlowLineChart(
    flowPoints: List<MonthlyFlowPoint>,
    modifier: Modifier = Modifier,
    title: String = "Flusso Mensile",
    isPrivacyMode: Boolean = false
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    fun formatVal(amt: Double): String {
        return if (isPrivacyMode) "**** €" else String.format(Locale.ITALY, "%.0f €", amt)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendSquare(color = IncomeGreen, label = "Entrata")
                Spacer(modifier = Modifier.width(8.dp))
                LegendSquare(color = ExpenseRed, label = "Spesa")
                Spacer(modifier = Modifier.width(8.dp))
                LegendSquare(color = DifferenceBlue, label = "Diff.")
                Spacer(modifier = Modifier.width(8.dp))
                LegendSquare(color = PrimaryViolet, label = "Saldo")
                Spacer(modifier = Modifier.width(8.dp))
                LegendSquare(color = Color(0xFFF59E0B), label = "Trend Prec.", isDashed = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Popover Tooltip
            selectedPointIndex?.let { idx ->
                val p = flowPoints.getOrNull(idx)
                if (p != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = p.monthLabel,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(IncomeGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Entrata: ${formatVal(p.income)}", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(ExpenseRed))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Spesa: ${formatVal(p.expense)}", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(DifferenceBlue))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Differenza: ${formatVal(p.difference)}", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(PrimaryViolet))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saldo: ${formatVal(p.balance)}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (flowPoints.isEmpty()) {
                Box(
                    modifier = Modifier.height(180.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessun dato disponibile per il flusso mensile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val maxVal = flowPoints.flatMap { listOf(it.income, it.expense, it.difference, it.prevYearTrend, it.balance) }
                    .maxOrNull()?.coerceAtLeast(100.0) ?: 350.0
                val minVal = flowPoints.flatMap { listOf(it.income, it.expense, it.difference, it.prevYearTrend, it.balance) }
                    .minOrNull()?.coerceAtMost(-50.0) ?: -100.0
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(flowPoints) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width
                                    val spacing = w / (flowPoints.size - 1).coerceAtLeast(1)
                                    val nearestIdx = (tapOffset.x / spacing).toInt().coerceIn(0, flowPoints.size - 1)
                                    selectedPointIndex = nearestIdx
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val spacing = w / (flowPoints.size - 1).coerceAtLeast(1)

                        fun getY(valNum: Double): Float {
                            val norm = (valNum - minVal) / range
                            return h - (norm * h).toFloat()
                        }

                        // Zero Y Axis Line
                        val zeroY = getY(0.0)
                        drawLine(
                            color = BorderDark,
                            start = Offset(0f, zeroY),
                            end = Offset(w, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Smooth Bezier path generator function
                        fun drawCubicLine(
                            getPointValue: (MonthlyFlowPoint) -> Double,
                            color: Color,
                            strokeWidthDp: Float = 2.5f,
                            isDashed: Boolean = false
                        ) {
                            if (flowPoints.size < 2) return
                            val path = Path()

                            val pts = flowPoints.mapIndexed { i, p ->
                                Offset(i * spacing, getY(getPointValue(p)))
                            }

                            path.moveTo(pts[0].x, pts[0].y)
                            for (i in 0 until pts.size - 1) {
                                val p0 = pts[i]
                                val p1 = pts[i + 1]
                                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                            }

                            val pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = strokeWidthDp.dp.toPx(), pathEffect = pathEffect)
                            )

                            // Point Circles
                            pts.forEachIndexed { i, pt ->
                                val isSelected = selectedPointIndex == i
                                val r = if (isSelected) 5.5.dp.toPx() else 3.dp.toPx()
                                drawCircle(color = color, radius = r, center = pt)
                            }
                        }

                        // Draw lines
                        drawCubicLine({ it.income }, IncomeGreen, 2.5f)
                        drawCubicLine({ it.expense }, ExpenseRed, 2.5f)
                        drawCubicLine({ it.difference }, DifferenceBlue, 2.5f)
                        drawCubicLine({ it.balance }, PrimaryViolet, 2.5f)
                        drawCubicLine({ it.prevYearTrend }, Color(0xFFF59E0B), 2f, isDashed = true)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    flowPoints.forEach { pt ->
                        Text(
                            text = pt.monthLabel,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendSquare(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
