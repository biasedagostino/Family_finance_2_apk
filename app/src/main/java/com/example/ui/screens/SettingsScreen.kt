package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinancialViewModel
import com.example.ui.UiState
import com.example.ui.components.DatePickerField
import com.example.ui.theme.*
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FinancialViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Local form states
    var initialSavingsText by remember(uiState.initialSavings) { mutableStateOf(uiState.initialSavings.toString()) }
    var nextSalaryDateDisplay by remember(uiState.settings) {
        mutableStateOf(DateUtils.isoToDisplay(uiState.settings["Data Prossimo Stipendio"] ?: "2026-11-07"))
    }

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("Spesa") }

    // System File Pickers (SAF)
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsvToUri(context, it) }
    }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCsvFromUri(context, it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Valore Attuale Risparmi & Data Stipendio
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Valore Attuale Risparmi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inserisci quanto hai messo da parte ad oggi. Solo le transazioni da oggi in poi influiranno su questo valore nel Fondo Risparmio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = initialSavingsText,
                            onValueChange = { initialSavingsText = it },
                            label = { Text("Importo Iniziale (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.saveSetting("Risparmio Iniziale", initialSavingsText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                        ) {
                            Text("Salva")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Data Prossimo Stipendio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DatePickerField(
                            valueDisplay = nextSalaryDateDisplay,
                            onDateSelected = { nextSalaryDateDisplay = it },
                            label = "Data (gg/mm/aaaa)",
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                viewModel.saveSetting("Data Prossimo Stipendio", nextSalaryDateDisplay)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                        ) {
                            Text("Aggiorna")
                        }
                    }
                }
            }
        }

        // Direct CSV File Import / Export (Storage Access Framework)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Import / Export File CSV Diretto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Salva o carica direttamente un file .csv dal dispositivo utilizzando il selettore di file di sistema Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                createCsvLauncher.launch("portale_finanziario_backup.csv")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salva File CSV", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                openCsvLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Carica File CSV", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Aggiungi Nuova Categoria Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aggiungi Nuova Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nome Categoria (es. Salute, Shopping)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = newCategoryType == "Spesa",
                            onClick = { newCategoryType = "Spesa" },
                            label = { Text("Spesa") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newCategoryType == "Entrata",
                            onClick = { newCategoryType = "Entrata" },
                            label = { Text("Entrata") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            if (newCategoryName.isNotEmpty()) {
                                viewModel.addCategory(newCategoryName.trim(), newCategoryType)
                                newCategoryName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                    ) {
                        Text("+ Aggiungi Categoria")
                    }
                }
            }
        }

        // Categorie Esistenti Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Categorie Esistenti",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f))
                        Text("TIPO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("BUDGET PERS.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("RISPARMIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("AZIONI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(48.dp))
                    }
                    Divider(color = BorderDark, thickness = 1.dp)

                    uiState.categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(2f)
                            )

                            val typeColor = if (cat.type.equals("Entrata", ignoreCase = true)) IncomeGreen else ExpenseRed
                            Text(
                                text = cat.type,
                                fontSize = 11.sp,
                                color = typeColor,
                                modifier = Modifier.weight(1f)
                            )

                            RadioButton(
                                selected = cat.useForPersonalBudget,
                                onClick = { viewModel.setPersonalBudgetCategory(cat.name) },
                                modifier = Modifier.weight(1f)
                            )

                            Checkbox(
                                checked = cat.useForSavings,
                                onCheckedChange = { isChecked -> viewModel.toggleSavingsCategory(cat.name, isChecked) },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.deleteCategory(cat.name) },
                                modifier = Modifier.width(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Elimina",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Divider(color = BorderDark, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
