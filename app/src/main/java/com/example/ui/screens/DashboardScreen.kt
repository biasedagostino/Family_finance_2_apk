package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinancialViewModel
import com.example.ui.UiState
import com.example.ui.components.CollapsibleCard
import com.example.ui.components.DatePickerField
import com.example.ui.components.DonutChart
import com.example.ui.components.FlowLineChart
import com.example.ui.components.InteractiveProjectionLineChart
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    var isAddFormExpanded by remember { mutableStateOf(false) }

    // Form states
    var dateDisplay by remember { mutableStateOf("05/10/2026") }
    var descriptionText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Spesa") }
    var selectedCategory by remember { mutableStateOf("") }
    var isFutureToggle by remember { mutableStateOf(false) }

    // Recurrence states
    var isRecurring by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("Mensile") }
    var repetitionsText by remember { mutableStateOf("6") }
    var dayRefText by remember { mutableStateOf("5") }

    // Category View Toggle (Single Month vs Overall Historical)
    var showHistoricalCategories by remember { mutableStateOf(false) }

    // Specific Month Filters
    var historyMonthFilter by remember { mutableStateOf("Tutti") }
    var futureMonthFilter by remember { mutableStateOf("Tutti") }

    // Table Pagination
    var historyPage by remember { mutableIntStateOf(1) }
    val historyItemsPerPage = 10

    var futurePage by remember { mutableIntStateOf(1) }
    val futureItemsPerPage = 10

    LaunchedEffect(uiState.categories) {
        if (selectedCategory.isEmpty() && uiState.categories.isNotEmpty()) {
            selectedCategory = uiState.categories.first().name
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top Metric Cards (ALWAYS VISIBLE - NOT COLLAPSIBLE) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Riepilogo Saldo e Proiezioni",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SALDO ATTUALE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = viewModel.formatAmount(uiState.currentBalance),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryLightViolet
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PROIEZIONE FINE MESE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Data: ${uiState.endOfMonthDateDisplay}",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val color = if (uiState.projectionEndOfMonth >= 0) IncomeGreen else ExpenseRed
                                    Text(
                                        text = viewModel.formatAmount(uiState.projectionEndOfMonth),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PROIEZIONE STIPENDIO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Data: ${uiState.nextSalaryDateDisplay}",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val color = if (uiState.projectionNextSalary >= 0) IncomeGreen else ExpenseRed
                                    Text(
                                        text = viewModel.formatAmount(uiState.projectionNextSalary),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Dedicated Interactive Projection Line Chart (ALWAYS VISIBLE - NOT COLLAPSIBLE) ---
        item {
            InteractiveProjectionLineChart(
                points = uiState.projectionPoints,
                endOfMonthDateDisplay = uiState.endOfMonthDateDisplay,
                nextSalaryDateDisplay = uiState.nextSalaryDateDisplay
            )
        }

        // --- 3. Card Previsione Spese Mensili (Collapsible Closed by default) ---
        item {
            CollapsibleCard(
                title = "Previsione Spese Mensili BI",
                subtitle = "Calcolo reattivo in tempo reale basato sulle scadenze programmate.",
                containerColor = Color(0xFF1E1B4B),
                initialExpanded = false
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Previsione Mese (${uiState.selectedMonthKey}): ${viewModel.formatAmount(uiState.monthlyPredictionExpenses)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }
        }

        // --- 4. Form Aggiungi Nuova Transazione (Collapsible Closed by default) ---
        item {
            CollapsibleCard(title = "Aggiungi Nuova Transazione", initialExpanded = false) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatePickerField(
                            valueDisplay = dateDisplay,
                            onDateSelected = { dateDisplay = it },
                            label = "Data (gg/mm/aaaa)",
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Importo") },
                            placeholder = { Text("50.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Descrizione") },
                        placeholder = { Text("es. Pranzo, Caffè") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val filteredCategories = remember(uiState.categories, selectedType) {
                        uiState.categories.filter { it.type.equals(selectedType, ignoreCase = true) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "Spesa",
                            onClick = {
                                if (selectedType != "Spesa") {
                                    selectedType = "Spesa"
                                    selectedCategory = ""
                                }
                            },
                            label = { Text("Spesa") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == "Entrata",
                            onClick = {
                                if (selectedType != "Entrata") {
                                    selectedType = "Entrata"
                                    selectedCategory = ""
                                }
                            },
                            label = { Text("Entrata") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Categoria:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory.ifEmpty { "Seleziona una categoria (${selectedType})" })
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            if (filteredCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nessuna categoria per $selectedType") },
                                    onClick = { dropdownExpanded = false }
                                )
                            } else {
                                filteredCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategory = cat.name
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                        Text(
                            text = "Ripeti questa transazione",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isRecurring) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var freqExpanded by remember { mutableStateOf(false) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("FREQUENZA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Box {
                                        OutlinedButton(
                                            onClick = { freqExpanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(frequency, fontSize = 11.sp)
                                        }
                                        DropdownMenu(
                                            expanded = freqExpanded,
                                            onDismissRequest = { freqExpanded = false }
                                        ) {
                                            listOf("Mensile", "Annuale", "Settimanale").forEach { f ->
                                                DropdownMenuItem(
                                                    text = { Text(f) },
                                                    onClick = {
                                                        frequency = f
                                                        freqExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("NUMERO DI RIPETIZIONI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    OutlinedTextField(
                                        value = repetitionsText,
                                        onValueChange = { repetitionsText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("RIFERIMENTO GIORNO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    OutlinedTextField(
                                        value = dayRefText,
                                        onValueChange = { dayRefText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            val reps = repetitionsText.toIntOrNull() ?: 1
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val totalRecurringAmount = amt * reps
                            val dayRef = dayRefText.toIntOrNull() ?: 5
                            val lastDateDisplay = DateUtils.calculateLastRepetitionDate(dateDisplay, frequency, reps)

                            Text(
                                text = "Ogni giorno $dayRef del mese",
                                fontSize = 11.sp,
                                color = PrimaryLightViolet,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Totale complessivo: ${viewModel.formatAmount(totalRecurringAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ULTIMA RIPETIZIONE PREVISTA: $lastDateDisplay",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isFutureToggle,
                                onCheckedChange = { isFutureToggle = it }
                            )
                            Text("Transazione Programmata / Futura", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (descriptionText.isNotEmpty() && amt > 0.0 && selectedCategory.isNotEmpty()) {
                                if (isRecurring) {
                                    val reps = repetitionsText.toIntOrNull() ?: 1
                                    val dayRef = dayRefText.toIntOrNull() ?: 5
                                    viewModel.addRecurringTransaction(
                                        startDateDisplay = dateDisplay,
                                        description = descriptionText,
                                        amount = amt,
                                        type = selectedType,
                                        category = selectedCategory,
                                        frequency = frequency,
                                        repetitions = reps,
                                        dayRef = dayRef
                                    )
                                } else {
                                    viewModel.addTransaction(
                                        dateDisplay = dateDisplay,
                                        description = descriptionText,
                                        amount = amt,
                                        type = selectedType,
                                        category = selectedCategory,
                                        isFuture = isFutureToggle
                                    )
                                }
                                descriptionText = ""
                                amountText = ""
                                isAddFormExpanded = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                    ) {
                        Text("+ Aggiungi Transazione")
                    }
                }
            }
        }

        // --- 5. Flusso Mensile Line Chart (Exactly Last 12 Months) ---
        item {
            CollapsibleCard(title = "Flusso Mensile Storico (Ultimi 12 Mesi)", initialExpanded = false) {
                FlowLineChart(
                    flowPoints = uiState.monthlyFlowPoints,
                    isPrivacyMode = uiState.isPrivacyModeEnabled
                )
            }
        }

        // --- 6. Donut Charts Section (With Alternative Button for Overall Historical View) ---
        item {
            CollapsibleCard(title = "Ripartizione Categorie", initialExpanded = false) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Control Row: Month/Year Dropdown vs Historical Toggle Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showHistoricalCategories,
                            onClick = { showHistoricalCategories = !showHistoricalCategories },
                            label = { Text("Storico Complessivo", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = Color.White
                            )
                        )

                        if (!showHistoricalCategories) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mese:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                var catMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { catMenuExpanded = true }) {
                                        Text(uiState.selectedMonthKey, fontSize = 11.sp)
                                    }
                                    DropdownMenu(
                                        expanded = catMenuExpanded,
                                        onDismissRequest = { catMenuExpanded = false }
                                    ) {
                                        uiState.availableMonths.reversed().forEach { mKey ->
                                            DropdownMenuItem(
                                                text = { Text(mKey) },
                                                onClick = {
                                                    viewModel.setSelectedMonth(mKey)
                                                    catMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (showHistoricalCategories) {
                        DonutChart(
                            title = "Categorie di Spesa (Storico Complessivo)",
                            aggregates = uiState.historicalExpenseCategories
                        )

                        DonutChart(
                            title = "Categorie di Entrata (Storico Complessivo)",
                            aggregates = uiState.historicalIncomeCategories
                        )
                    } else {
                        DonutChart(
                            title = "Categorie di Spesa (${uiState.selectedMonthKey})",
                            aggregates = uiState.selectedMonthExpenseCategories
                        )

                        DonutChart(
                            title = "Categorie di Entrata (${uiState.selectedMonthKey})",
                            aggregates = uiState.selectedMonthIncomeCategories
                        )
                    }
                }
            }
        }

        // --- 7. Storico Transazioni Table (With Month/Year Selector) ---
        item {
            CollapsibleCard(title = "Storico Transazioni", initialExpanded = false) {
                val allNonFuture = uiState.transactions.filter { !it.isFuture }

                // Month/Year Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtra Mese/Anno:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    var filterMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { filterMenuExpanded = true }) {
                            Text(if (historyMonthFilter == "Tutti") "Tutti i mesi" else historyMonthFilter, fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tutti i mesi") },
                                onClick = {
                                    historyMonthFilter = "Tutti"
                                    filterMenuExpanded = false
                                }
                            )
                            uiState.availableMonths.reversed().forEach { mKey ->
                                DropdownMenuItem(
                                    text = { Text(mKey) },
                                    onClick = {
                                        historyMonthFilter = mKey
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filteredNonFuture = if (historyMonthFilter == "Tutti") allNonFuture else allNonFuture.filter { it.date.startsWith(historyMonthFilter) }

                val totalPages = (filteredNonFuture.size + historyItemsPerPage - 1) / historyItemsPerPage
                val safePage = historyPage.coerceIn(1, totalPages.coerceAtLeast(1))
                val pagedItems = filteredNonFuture.drop((safePage - 1) * historyItemsPerPage).take(historyItemsPerPage)

                Column {
                    if (filteredNonFuture.isEmpty()) {
                        Text(
                            text = "Nessuna transazione trovata per il mese selezionato",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        pagedItems.forEach { tx ->
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

                                val isInc = tx.type.equals("Entrata", ignoreCase = true)
                                val prefix = if (isInc) "+ " else "- "
                                val color = if (isInc) IncomeGreen else ExpenseRed

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$prefix${viewModel.formatAmount(tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 14.sp
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Divider(color = BorderDark, thickness = 0.5.dp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (historyPage > 1) historyPage-- },
                                enabled = historyPage > 1
                            ) {
                                Text("Precedente", fontSize = 11.sp)
                            }

                            Text(
                                text = "Pagina $safePage di ${totalPages.coerceAtLeast(1)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = { if (historyPage < totalPages) historyPage++ },
                                enabled = historyPage < totalPages
                            ) {
                                Text("Successivo", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 8. Transazioni Programmate Table (Sorted Chronologically ASC: Nearest Future Date First) ---
        item {
            CollapsibleCard(title = "Transazioni Programmate", initialExpanded = false) {
                // Sorted ascending by date so the nearest future date (e.g. tomorrow) appears first down to furthest future date
                val allFuture = uiState.transactions.filter { it.isFuture }.sortedBy { it.date }

                // Month/Year Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtra Mese/Anno:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    var filterMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { filterMenuExpanded = true }) {
                            Text(if (futureMonthFilter == "Tutti") "Tutti i mesi" else futureMonthFilter, fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tutti i mesi") },
                                onClick = {
                                    futureMonthFilter = "Tutti"
                                    filterMenuExpanded = false
                                }
                            )
                            uiState.availableMonths.reversed().forEach { mKey ->
                                DropdownMenuItem(
                                    text = { Text(mKey) },
                                    onClick = {
                                        futureMonthFilter = mKey
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filteredFuture = if (futureMonthFilter == "Tutti") allFuture else allFuture.filter { it.date.startsWith(futureMonthFilter) }

                val totalPages = (filteredFuture.size + futureItemsPerPage - 1) / futureItemsPerPage
                val safePage = futurePage.coerceIn(1, totalPages.coerceAtLeast(1))
                val pagedItems = filteredFuture.drop((safePage - 1) * futureItemsPerPage).take(futureItemsPerPage)

                if (filteredFuture.isEmpty()) {
                    Text(
                        text = "Nessuna transazione programmata trovata per il mese selezionato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column {
                        pagedItems.forEach { tx ->
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

                                val isInc = tx.type.equals("Entrata", ignoreCase = true)
                                val color = if (isInc) IncomeGreen else ExpenseRed

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viewModel.formatAmount(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 14.sp
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Divider(color = BorderDark, thickness = 0.5.dp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (futurePage > 1) futurePage-- },
                                enabled = futurePage > 1
                            ) {
                                Text("Precedente", fontSize = 11.sp)
                            }

                            Text(
                                text = "Pagina $safePage di ${totalPages.coerceAtLeast(1)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = { if (futurePage < totalPages) futurePage++ },
                                enabled = futurePage < totalPages
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
