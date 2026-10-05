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
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.util.*

data class ProjectionPoint(
    val dateIso: String,      // "YYYY-MM-DD"
    val dateDisplay: String,  // "DD/MM/YYYY"
    val balance: Double,
    val isEndOfMonth: Boolean = false,
    val isNextSalary: Boolean = false
)

@Composable
fun InteractiveProjectionLineChart(
    points: List<ProjectionPoint>,
    endOfMonthDateDisplay: String,
    nextSalaryDateDisplay: String,
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

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
                text = "Grafico Proiezione Saldo & Stipendio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fine Mese: $endOfMonthDateDisplay | Stipendio: $nextSalaryDateDisplay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooltip preview when point selected
            selectedPointIndex?.let { idx ->
                val p = points.getOrNull(idx)
                if (p != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryViolet)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${p.dateDisplay}: ${String.format(Locale.ITALY, "%.2f €", p.balance)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calcolo proiezioni in corso...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxBal = points.maxOf { it.balance }.coerceAtLeast(100.0)
                val minBal = points.minOf { it.balance }.coerceAtMost(0.0)
                val range = (maxBal - minBal).coerceAtLeast(1.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width
                                    val spacing = w / (points.size - 1).coerceAtLeast(1)
                                    val nearestIndex = (tapOffset.x / spacing).toInt().coerceIn(0, points.size - 1)
                                    selectedPointIndex = nearestIndex
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val spacing = w / (points.size - 1).coerceAtLeast(1)

                        fun getY(bal: Double): Float {
                            val norm = (bal - minBal) / range
                            return h - (norm * h).toFloat()
                        }

                        // Zero line
                        val zeroY = getY(0.0)
                        drawLine(
                            color = BorderDark,
                            start = Offset(0f, zeroY),
                            end = Offset(w, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Path trajectory
                        val path = Path()
                        points.forEachIndexed { i, p ->
                            val x = i * spacing
                            val y = getY(p.balance)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                            // Vertical marker lines for End of Month and Next Salary
                            if (p.isEndOfMonth) {
                                drawLine(
                                    color = DifferenceBlue,
                                    start = Offset(x, 0f),
                                    end = Offset(x, h),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            }
                            if (p.isNextSalary) {
                                drawLine(
                                    color = IncomeGreen,
                                    start = Offset(x, 0f),
                                    end = Offset(x, h),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                                )
                            }
                        }

                        drawPath(
                            path = path,
                            color = PrimaryLightViolet,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw Point circles
                        points.forEachIndexed { i, p ->
                            val x = i * spacing
                            val y = getY(p.balance)
                            val isSelected = selectedPointIndex == i

                            val ptColor = when {
                                p.isNextSalary -> IncomeGreen
                                p.isEndOfMonth -> DifferenceBlue
                                isSelected -> Color.White
                                else -> PrimaryViolet
                            }

                            val radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()
                            drawCircle(color = ptColor, radius = radius, center = Offset(x, y))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Labels Row (Start, End of Month, Next Salary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = points.firstOrNull()?.dateDisplay ?: "",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Fine Mese: $endOfMonthDateDisplay",
                        fontSize = 10.sp,
                        color = DifferenceBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stipendio: $nextSalaryDateDisplay",
                        fontSize = 10.sp,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
