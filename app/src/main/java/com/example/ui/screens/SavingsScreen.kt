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
import com.example.ui.FinancialViewModel
import com.example.ui.UiState
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.util.*

@Composable
fun SavingsScreen(
    viewModel: FinancialViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableIntStateOf(1) }
    val itemsPerPage = 10

    val movements = uiState.savingsMovements
    val totalPages = (movements.size + itemsPerPage - 1) / itemsPerPage
    val safePage = page.coerceIn(1, totalPages.coerceAtLeast(1))
    val pagedMovements = movements.drop((safePage - 1) * itemsPerPage).take(itemsPerPage)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Risparmi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Top Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Fondo Risparmio Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FONDO RISPARMIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLightViolet
                            )
                            Text(
                                text = "Inizializzato il: ${DateUtils.isoToDisplay(uiState.settings["Data Iniziale Risparmio"] ?: "2026-03-13")}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.formatAmount(uiState.currentSavingsFund),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                                text = "TOTALE VERSAMENTI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.formatAmount(uiState.totalSavingsDeposits),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
                                text = "TOTALE PRELIEVI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.formatAmount(uiState.totalSavingsWithdrawals),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Informative Note
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Il fondo risparmio calcola il valore iniziale più i versamenti meno i prelievi effettuati sulle categorie contrassegnate come Risparmio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Movimenti Risparmio Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Movimenti Risparmio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (movements.isEmpty()) {
                        Text(
                            text = "Nessun movimento di risparmio registrato",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pagedMovements.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.description,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${DateUtils.isoToDisplay(tx.date)} • ${tx.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val isDeposit = tx.type.equals("Spesa", ignoreCase = true)
                                val label = if (isDeposit) "Versamenti (Spese)" else "Prelievi (Entrate)"
                                val color = if (isDeposit) IncomeGreen else ExpenseRed

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = viewModel.formatAmount(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = color
                                    )
                                }
                            }
                            Divider(color = BorderDark, thickness = 0.5.dp)
                        }

                        // Pagination
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (page > 1) page-- },
                                enabled = page > 1
                            ) {
                                Text("Precedente", fontSize = 11.sp)
                            }

                            Text(
                                text = "Pagina $safePage di ${totalPages.coerceAtLeast(1)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = { if (page < totalPages) page++ },
                                enabled = page < totalPages
                            ) {
                                Text("Successivo", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
