package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryAggregate
import com.example.ui.FinancialViewModel
import com.example.ui.UiState
import com.example.ui.components.CategoryBarChart
import com.example.ui.components.DonutChart
import com.example.ui.theme.*
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: FinancialViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    val dateFilter by viewModel.biDateFilter.collectAsState()
    val filterOptions = listOf("Mese Corrente", "3 Mesi", "6 Mesi", "YTD", "Tutto")

    val filteredTxs = remember(uiState.transactions, dateFilter) {
        val nonFuture = uiState.transactions.filter { !it.isFuture }
        when (dateFilter) {
            "Mese Corrente" -> nonFuture.filter { it.date.startsWith(uiState.selectedMonthKey) }
            "3 Mesi" -> nonFuture.filter { it.date >= "2026-08-01" }
            "6 Mesi" -> nonFuture.filter { it.date >= "2026-05-01" }
            "YTD" -> nonFuture.filter { it.date.startsWith("2026") }
            else -> nonFuture
        }
    }

    val totalExpense = filteredTxs.filter { it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
    val totalIncome = filteredTxs.filter { it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }

    val expCategories = remember(filteredTxs) {
        val totalExp = filteredTxs.filter { it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        filteredTxs.filter { it.type.equals("Spesa", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, items) ->
                val amt = items.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / totalExp * 100).toFloat())
            }.sortedByDescending { it.amount }
    }

    val incCategories = remember(filteredTxs) {
        val totalInc = filteredTxs.filter { it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        filteredTxs.filter { it.type.equals("Entrata", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, items) ->
                val amt = items.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / totalInc * 100).toFloat())
            }.sortedByDescending { it.amount }
    }

    val topCategoryName = expCategories.firstOrNull()?.categoryName ?: "N/D"
    val topCategoryAmount = expCategories.firstOrNull()?.amount ?: 0.0

    val distinctMonthsCount = filteredTxs.map { it.date.take(7) }.distinct().size.coerceAtLeast(1)
    val avgMonthlyExpense = totalExpense / distinctMonthsCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Business Intelligence",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Date Range Filters Chips
        item {
            Column {
                Text(
                    text = "Intervallo Temporale:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { opt ->
                        FilterChip(
                            selected = dateFilter == opt,
                            onClick = { viewModel.setBiDateFilter(opt) },
                            label = { Text(opt, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Autonomia Finanziaria & YoY Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Autonomia Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AUTONOMIA FINANZIARIA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F3D0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.ITALY, "%.1f Mesi", uiState.financialAutonomiaMonths),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "di copertura delle spese medie",
                            fontSize = 8.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                // YoY Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CONFRONTO YOY (${uiState.selectedMonthKey})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val yoy = uiState.yoyComparison
                        val color = if (yoy.percentChange <= 0) IncomeGreen else ExpenseRed
                        val sign = if (yoy.percentChange > 0) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.ITALY, "%.1f%%", yoy.percentChange)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "rispetto allo stesso mese anno prec.",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Key Insights Grid Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "MEDIA SPESA MENSILE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(avgMonthlyExpense),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TOP CATEGORIA SPESA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topCategoryName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLightViolet,
                                maxLines = 1
                            )
                            Text(
                                text = viewModel.formatAmount(topCategoryAmount),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TOTALE ENTRATE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(totalIncome),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TOTALE SPESE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(totalExpense),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Tasso di Risparmio % Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tasso di Risparmio % Mensile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.savingsRatePoints.takeLast(6).forEach { pt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pt.monthLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val color = if (pt.ratePercent >= 0) IncomeGreen else ExpenseRed
                            Text(
                                text = String.format(Locale.ITALY, "%.1f%%", pt.ratePercent),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Divider(color = BorderDark, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Bar Chart of Top Expenses
        item {
            CategoryBarChart(
                title = "Analisi Categorie di Spesa ($dateFilter)",
                aggregates = expCategories
            )
        }

        // Donut Chart Expenses
        item {
            DonutChart(
                title = "Distribuzione Categorie di Spesa ($dateFilter)",
                aggregates = expCategories
            )
        }

        // Bar Chart of Top Income Categories
        item {
            CategoryBarChart(
                title = "Analisi Categorie di Entrata ($dateFilter)",
                aggregates = incCategories
            )
        }

        // Donut Chart Income Categories
        item {
            DonutChart(
                title = "Distribuzione Categorie di Entrata ($dateFilter)",
                aggregates = incCategories
            )
        }
    }
}
