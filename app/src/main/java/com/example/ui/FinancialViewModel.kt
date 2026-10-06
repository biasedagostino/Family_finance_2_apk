package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.FinancialRepository
import com.example.ui.components.ProjectionPoint
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class MonthlyFlowPoint(
    val monthLabel: String,
    val yearMonthKey: String, // "YYYY-MM"
    val income: Double,
    val expense: Double,
    val difference: Double,
    val prevYearTrend: Double,
    val balance: Double = 0.0
)

data class CategoryAggregate(
    val categoryName: String,
    val amount: Double,
    val percentage: Float
)

data class SavingsRatePoint(
    val monthLabel: String,
    val ratePercent: Double
)

data class YoyComparison(
    val currentMonthTotal: Double,
    val prevYearMonthTotal: Double,
    val percentChange: Double
)

data class CategoryTrendPoint(
    val monthLabel: String,
    val foodAmount: Double,
    val billsAmount: Double,
    val fuelAmount: Double
)

data class UiState(
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val personalDetails: List<PersonalDetailEntity> = emptyList(),
    val settings: Map<String, String> = emptyMap(),

    // Privacy Mode (Default True)
    val isPrivacyModeEnabled: Boolean = true,

    // Computed Financial Metrics
    val currentBalance: Double = 0.0,
    val projectionEndOfMonth: Double = 0.0,
    val projectionNextSalary: Double = 0.0,
    val monthlyPredictionExpenses: Double = 0.0,

    // Dynamic Projection Points
    val projectionPoints: List<ProjectionPoint> = emptyList(),
    val endOfMonthDateDisplay: String = "31/10/2026",
    val nextSalaryDateDisplay: String = "07/11/2026",

    // Savings Metrics
    val initialSavings: Double = 0.0,
    val totalSavingsDeposits: Double = 0.0,
    val totalSavingsWithdrawals: Double = 0.0,
    val currentSavingsFund: Double = 0.0,
    val savingsMovements: List<TransactionEntity> = emptyList(),

    // Personal Budget Metrics
    val personalBudgetCategory: String = "Mensile Gino",
    val personalAllocation: Double = 0.0,
    val personalSpent: Double = 0.0,
    val personalRemaining: Double = 0.0,
    val personalDailyProjection: Double = 0.0,

    // New BI Metrics & Charts
    val financialAutonomiaMonths: Double = 0.0,
    val savingsRatePoints: List<SavingsRatePoint> = emptyList(),
    val yoyComparison: YoyComparison = YoyComparison(0.0, 0.0, 0.0),
    val categoryTrendPoints: List<CategoryTrendPoint> = emptyList(),

    // Charts Data
    val monthlyFlowPoints: List<MonthlyFlowPoint> = emptyList(),
    val historicalExpenseCategories: List<CategoryAggregate> = emptyList(),
    val historicalIncomeCategories: List<CategoryAggregate> = emptyList(),
    val selectedMonthExpenseCategories: List<CategoryAggregate> = emptyList(),
    val selectedMonthIncomeCategories: List<CategoryAggregate> = emptyList(),
    val availableMonths: List<String> = emptyList(),
    val selectedMonthKey: String = "2026-10",

    // Personal Details Charts
    val personalMonthlyFlow: List<MonthlyFlowPoint> = emptyList(),
    val personalHistoricalCategories: List<CategoryAggregate> = emptyList(),
    val personalMonthCategories: List<CategoryAggregate> = emptyList(),

    val isLoading: Boolean = true
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinancialRepository(AppDatabase.getDatabase(application))

    private val _selectedMonthKey = MutableStateFlow("2026-10")
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _biDateFilter = MutableStateFlow("Tutto")
    val biDateFilter: StateFlow<String> = _biDateFilter.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(true)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty(getApplication())

            val baseFlow = combine(
                repository.categories,
                repository.transactions,
                repository.personalDetails,
                repository.settings
            ) { cats, txs, pds, sts ->
                Tuple4(cats, txs, pds, sts.associate { it.key to it.value })
            }

            combine(
                baseFlow,
                _selectedMonthKey,
                _biDateFilter,
                _isPrivacyModeEnabled
            ) { tuple, selMonth, biFilter, privacy ->
                calculateUiState(tuple.cats, tuple.txs, tuple.pds, tuple.settings, selMonth, biFilter, privacy)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private data class Tuple4<A, B, C, D>(
        val cats: A,
        val txs: B,
        val pds: C,
        val settings: D
    )

    fun togglePrivacyMode() {
        _isPrivacyModeEnabled.value = !_isPrivacyModeEnabled.value
    }

    fun formatAmount(amount: Double): String {
        return if (_isPrivacyModeEnabled.value) {
            "**** €"
        } else {
            String.format(Locale.ITALY, "%.2f €", amount)
        }
    }

    private fun calculateUiState(
        cats: List<CategoryEntity>,
        txs: List<TransactionEntity>,
        pds: List<PersonalDetailEntity>,
        settings: Map<String, String>,
        selectedMonth: String,
        biFilter: String,
        privacyMode: Boolean
    ): UiState {
        val todayIso = "2026-10-05"
        val currentBal = txs.filter { !it.isFuture && it.date <= todayIso }.sumOf {
            if (it.type.equals("Entrata", ignoreCase = true)) it.amount else -it.amount
        }

        val nextSalaryDateIso = settings["Data Prossimo Stipendio"] ?: "2026-11-07"
        val nextSalaryDateDisplay = DateUtils.isoToDisplay(nextSalaryDateIso)

        val selParts = selectedMonth.split("-")
        val yearInt = selParts.getOrNull(0)?.toIntOrNull() ?: 2026
        val monthInt = selParts.getOrNull(1)?.toIntOrNull() ?: 10

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, monthInt - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val endOfMonthIso = String.format(Locale.ITALY, "%04d-%02d-%02d", yearInt, monthInt, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfMonthDateDisplay = DateUtils.isoToDisplay(endOfMonthIso)

        val futureMonthEndTxs = txs.filter { (it.isFuture || it.date > todayIso) && it.date <= endOfMonthIso }
        val projEndOfMonth = currentBal + futureMonthEndTxs.sumOf {
            if (it.type.equals("Entrata", ignoreCase = true)) it.amount else -it.amount
        }

        val futureNextSalaryTxs = txs.filter { (it.isFuture || it.date > todayIso) && it.date <= nextSalaryDateIso }
        val projNextSalary = currentBal + futureNextSalaryTxs.sumOf {
            if (it.type.equals("Entrata", ignoreCase = true)) it.amount else -it.amount
        }

        // Trajectory
        val maxTargetIso = if (nextSalaryDateIso > endOfMonthIso) nextSalaryDateIso else endOfMonthIso
        val projTrajectoryPoints = mutableListOf<ProjectionPoint>()
        val trajCal = DateUtils.parseToCalendar(todayIso)
        val targetCal = DateUtils.parseToCalendar(maxTargetIso)

        var runningBal = currentBal
        val sdfIso = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)

        while (!trajCal.after(targetCal)) {
            val currIso = sdfIso.format(trajCal.time)
            if (currIso > todayIso) {
                val dueOnDay = txs.filter { it.date == currIso }
                dueOnDay.forEach { t ->
                    if (t.type.equals("Entrata", ignoreCase = true)) runningBal += t.amount
                    else runningBal -= t.amount
                }
            }

            projTrajectoryPoints.add(
                ProjectionPoint(
                    dateIso = currIso,
                    dateDisplay = DateUtils.isoToDisplay(currIso),
                    balance = runningBal,
                    isEndOfMonth = (currIso == endOfMonthIso),
                    isNextSalary = (currIso == nextSalaryDateIso)
                )
            )
            trajCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Monthly Expenses Prediction
        val currentMonthExpenses = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
        val futureScheduledThisMonth = txs.filter { it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
        val predictedMonthlyExpenses = currentMonthExpenses + futureScheduledThisMonth

        // Savings Fund Calculation (Explicit user rule)
        // 1. Determine savings categories
        val settingsSavingsCats = settings["Categorie Risparmio"]?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        val dbSavingsCats = cats.filter { it.useForSavings }.map { it.name }.toSet()
        val savingsCategoryNames = settingsSavingsCats + dbSavingsCats

        val initialSavings = settings["Risparmio Iniziale"]?.toDoubleOrNull() ?: 0.0

        // 2. Realized completed movements on savings categories
        val savingsMovements = txs.filter {
            !it.isFuture &&
            it.date <= todayIso &&
            savingsCategoryNames.contains(it.category)
        }

        // Spesa on a savings category -> Increases savings (+ totalDeposits)
        val totalDeposits = savingsMovements.filter { it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }

        // Entrata on a savings category -> Decreases savings (- totalWithdrawals)
        val totalWithdrawals = savingsMovements.filter { it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }

        val currentSavingsFund = initialSavings + totalDeposits - totalWithdrawals

        // Personal Budget
        val personalCat = settings["Categoria Budget Personale"] ?: "Mensile Gino"
        val personalAllocation = txs.filter { !it.isFuture && it.category.equals(personalCat, ignoreCase = true) }.sumOf { it.amount }
            .let { if (it == 0.0) 1560.0 else it }
        val personalSpent = pds.sumOf { it.amount }
        val personalRemaining = personalAllocation - personalSpent

        val daysRemaining = try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
            val salaryCal = Calendar.getInstance().apply { time = sdf.parse(nextSalaryDateIso) ?: Date() }
            val nowCal = Calendar.getInstance().apply { time = sdf.parse(todayIso) ?: Date() }
            val diffMs = salaryCal.timeInMillis - nowCal.timeInMillis
            val days = (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            days
        } catch (e: Exception) {
            34
        }
        val personalDailyProj = if (daysRemaining > 0) personalRemaining / daysRemaining else 0.0

        // Exactly Last 12 Months ending at Current Month ("2026-10")
        val currentMonthKey = "2026-10"
        val last12MonthsList = generateLast12MonthKeys(currentMonthKey)

        val flowPoints = last12MonthsList.map { ymKey ->
            val monthTxs = txs.filter { !it.isFuture && it.date.startsWith(ymKey) }
            val inc = monthTxs.filter { it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
            val diff = inc - exp

            val prevYearKey = try {
                val parts = ymKey.split("-")
                "${parts[0].toInt() - 1}-${parts[1]}"
            } catch (e: Exception) { "" }

            val prevExp = txs.filter { !it.isFuture && it.date.startsWith(prevYearKey) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }

            MonthlyFlowPoint(
                monthLabel = formatMonthLabel(ymKey),
                yearMonthKey = ymKey,
                income = inc,
                expense = exp,
                difference = diff,
                prevYearTrend = prevExp
            )
        }

        // BI Metrics
        val allMonthsList = txs.map { it.date.take(7) }.distinct().sorted()
        val distinctMonthsCount = allMonthsList.size.coerceAtLeast(1)
        val avgMonthlyExp = txs.filter { !it.isFuture && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount } / distinctMonthsCount
        val totalLiquidity = currentBal + currentSavingsFund
        val autonomiaMonths = if (avgMonthlyExp > 0) totalLiquidity / avgMonthlyExp else 0.0

        val savingsRates = last12MonthsList.map { ymKey ->
            val monthTxs = txs.filter { !it.isFuture && it.date.startsWith(ymKey) }
            val inc = monthTxs.filter { it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
            val rate = if (inc > 0) ((inc - exp) / inc * 100).coerceIn(-100.0, 100.0) else 0.0
            SavingsRatePoint(formatMonthLabel(ymKey), rate)
        }

        // YoY Comparison
        val currentMonthExpTotal = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
        val prevYearMonthKey = try {
            val parts = selectedMonth.split("-")
            "${parts[0].toInt() - 1}-${parts[1]}"
        } catch (e: Exception) { "" }
        val prevYearMonthExpTotal = txs.filter { !it.isFuture && it.date.startsWith(prevYearMonthKey) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }
        val yoyPercentChange = if (prevYearMonthExpTotal > 0) ((currentMonthExpTotal - prevYearMonthExpTotal) / prevYearMonthExpTotal * 100) else 0.0
        val yoyComp = YoyComparison(currentMonthExpTotal, prevYearMonthExpTotal, yoyPercentChange)

        val catTrendList = last12MonthsList.takeLast(6).map { ymKey ->
            val mTxs = txs.filter { !it.isFuture && it.date.startsWith(ymKey) }
            val food = mTxs.filter { it.category.lowercase().contains("alimentare") }.sumOf { it.amount }
            val bills = mTxs.filter { it.category.lowercase().contains("bolletta") }.sumOf { it.amount }
            val fuel = mTxs.filter { it.category.lowercase().contains("carburante") }.sumOf { it.amount }
            CategoryTrendPoint(formatMonthLabel(ymKey), food, bills, fuel)
        }

        // Historic Expenses Aggregates
        val totalHistoricExp = txs.filter { !it.isFuture && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        val historicalExpCats = txs.filter { !it.isFuture && it.type.equals("Spesa", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, catTxs) ->
                val amt = catTxs.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / totalHistoricExp * 100).toFloat())
            }.sortedByDescending { it.amount }

        // Historic Income Aggregates
        val totalHistoricInc = txs.filter { !it.isFuture && it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        val historicalIncCats = txs.filter { !it.isFuture && it.type.equals("Entrata", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, catTxs) ->
                val amt = catTxs.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / totalHistoricInc * 100).toFloat())
            }.sortedByDescending { it.amount }

        // Selected Month Expense Aggregates
        val selMonthExpTotal = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Spesa", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        val selMonthExpCats = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Spesa", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, catTxs) ->
                val amt = catTxs.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / selMonthExpTotal * 100).toFloat())
            }.sortedByDescending { it.amount }

        // Selected Month Income Aggregates
        val selMonthIncTotal = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Entrata", ignoreCase = true) }.sumOf { it.amount }.coerceAtLeast(1.0)
        val selMonthIncCats = txs.filter { !it.isFuture && it.date.startsWith(selectedMonth) && it.type.equals("Entrata", ignoreCase = true) }
            .groupBy { it.category }
            .map { (catName, catTxs) ->
                val amt = catTxs.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / selMonthIncTotal * 100).toFloat())
            }.sortedByDescending { it.amount }

        // Personal Flow (Exactly Last 12 Months ending at Current Month)
        var runningPersonalBal = 0.0
        val personalFlow = last12MonthsList.map { ymKey ->
            val inc = txs.filter { !it.isFuture && it.date.startsWith(ymKey) && it.category.equals(personalCat, ignoreCase = true) }.sumOf { it.amount }
            val exp = pds.filter { it.date.startsWith(ymKey) }.sumOf { it.amount }
            val diff = inc - exp
            runningPersonalBal += diff

            val prevYearKey = try {
                val parts = ymKey.split("-")
                "${parts[0].toInt() - 1}-${parts[1]}"
            } catch (e: Exception) { "" }

            val prevExp = pds.filter { it.date.startsWith(prevYearKey) }.sumOf { it.amount }

            MonthlyFlowPoint(
                monthLabel = formatMonthLabel(ymKey),
                yearMonthKey = ymKey,
                income = inc,
                expense = exp,
                difference = diff,
                prevYearTrend = prevExp,
                balance = runningPersonalBal
            )
        }

        val totalPersonalHistExp = pds.sumOf { it.amount }.coerceAtLeast(1.0)
        val personalHistCats = pds.groupBy { it.category }
            .map { (catName, items) ->
                val amt = items.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / totalPersonalHistExp * 100).toFloat())
            }.sortedByDescending { it.amount }

        val selMonthPdTotal = pds.filter { it.date.startsWith(selectedMonth) }.sumOf { it.amount }.coerceAtLeast(1.0)
        val personalMonthCats = pds.filter { it.date.startsWith(selectedMonth) }
            .groupBy { it.category }
            .map { (catName, items) ->
                val amt = items.sumOf { it.amount }
                CategoryAggregate(catName, amt, (amt / selMonthPdTotal * 100).toFloat())
            }.sortedByDescending { it.amount }

        return UiState(
            categories = cats,
            transactions = txs,
            personalDetails = pds,
            settings = settings,
            isPrivacyModeEnabled = privacyMode,
            currentBalance = currentBal,
            projectionEndOfMonth = projEndOfMonth,
            projectionNextSalary = projNextSalary,
            monthlyPredictionExpenses = predictedMonthlyExpenses,
            projectionPoints = projTrajectoryPoints,
            endOfMonthDateDisplay = endOfMonthDateDisplay,
            nextSalaryDateDisplay = nextSalaryDateDisplay,
            initialSavings = initialSavings,
            totalSavingsDeposits = totalDeposits,
            totalSavingsWithdrawals = totalWithdrawals,
            currentSavingsFund = currentSavingsFund,
            savingsMovements = savingsMovements,
            personalBudgetCategory = personalCat,
            personalAllocation = personalAllocation,
            personalSpent = personalSpent,
            personalRemaining = personalRemaining,
            personalDailyProjection = personalDailyProj,
            financialAutonomiaMonths = autonomiaMonths,
            savingsRatePoints = savingsRates,
            yoyComparison = yoyComp,
            categoryTrendPoints = catTrendList,
            monthlyFlowPoints = flowPoints,
            historicalExpenseCategories = historicalExpCats,
            historicalIncomeCategories = historicalIncCats,
            selectedMonthExpenseCategories = selMonthExpCats,
            selectedMonthIncomeCategories = selMonthIncCats,
            availableMonths = last12MonthsList,
            selectedMonthKey = selectedMonth,
            personalMonthlyFlow = personalFlow,
            personalHistoricalCategories = personalHistCats,
            personalMonthCategories = personalMonthCats,
            isLoading = false
        )
    }

    private fun generateLast12MonthKeys(currentYmKey: String): List<String> {
        val result = mutableListOf<String>()
        val parts = currentYmKey.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 10
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, -11)
        val sdf = java.text.SimpleDateFormat("yyyy-MM", Locale.ITALY)
        for (i in 0 until 12) {
            result.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    fun setSelectedMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun setBiDateFilter(filter: String) {
        _biDateFilter.value = filter
    }

    fun addTransaction(
        dateDisplay: String,
        description: String,
        amount: Double,
        type: String,
        category: String,
        isFuture: Boolean
    ) {
        val dateIso = DateUtils.displayToIso(dateDisplay)
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    date = dateIso,
                    description = description,
                    amount = amount,
                    type = type,
                    category = category,
                    isFuture = isFuture
                )
            )
            Toast.makeText(getApplication(), "Transazione registrata!", Toast.LENGTH_SHORT).show()
        }
    }

    fun addRecurringTransaction(
        startDateDisplay: String,
        description: String,
        amount: Double,
        type: String,
        category: String,
        frequency: String,
        repetitions: Int,
        dayRef: Int
    ) {
        viewModelScope.launch {
            val datesIso = DateUtils.generateRecurringDates(startDateDisplay, frequency, repetitions, dayRef)
            val recId = "rec-${System.currentTimeMillis()}"

            datesIso.forEachIndexed { idx, dIso ->
                repository.addTransaction(
                    TransactionEntity(
                        date = dIso,
                        description = description,
                        amount = amount,
                        type = type,
                        category = category,
                        isFuture = (idx > 0 || dIso > "2026-10-05"),
                        recurrenceId = recId
                    )
                )
            }
            Toast.makeText(getApplication(), "$repetitions transazioni ricorrenti generate!", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            Toast.makeText(getApplication(), "Transazione eliminata", Toast.LENGTH_SHORT).show()
        }
    }

    fun addPersonalDetail(dateDisplay: String, description: String, category: String, amount: Double) {
        val dateIso = DateUtils.displayToIso(dateDisplay)
        viewModelScope.launch {
            repository.addPersonalDetail(
                PersonalDetailEntity(
                    date = dateIso,
                    description = description,
                    amount = amount,
                    type = "Spesa",
                    category = category
                )
            )
            Toast.makeText(getApplication(), "Spesa personale aggiunta!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePersonalDetail(id: Int) {
        viewModelScope.launch {
            repository.deletePersonalDetail(id)
            Toast.makeText(getApplication(), "Spesa personale eliminata", Toast.LENGTH_SHORT).show()
        }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.addCategory(CategoryEntity(name = name, type = type))
            Toast.makeText(getApplication(), "Categoria aggiunta!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
            Toast.makeText(getApplication(), "Categoria eliminata", Toast.LENGTH_SHORT).show()
        }
    }

    fun setPersonalBudgetCategory(name: String) {
        viewModelScope.launch {
            repository.setPersonalBudgetCategory(name)
            Toast.makeText(getApplication(), "Categoria budget personale aggiornata", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleSavingsCategory(categoryName: String, isSavings: Boolean) {
        viewModelScope.launch {
            repository.toggleSavingsCategory(categoryName, isSavings)
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            val formattedVal = if (key.lowercase().contains("stipendio") || key.lowercase().contains("data")) DateUtils.displayToIso(value) else value
            repository.saveSetting(key, formattedVal)
            Toast.makeText(getApplication(), "Impostazione salvata!", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportCsvToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvString = repository.exportCsvString()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csvString.toByteArray())
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "File CSV esportato con successo!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nell'esportazione: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun saveCsvToDownloads(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvString = repository.exportCsvString()
                val filename = "portale_finanziario_backup.csv"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(csvString.toByteArray())
                        }
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "File salvato in Download ($filename)!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        saveToExternalFiles(context, csvString, filename)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = java.io.File(downloadsDir, filename)
                    file.writeText(csvString)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "File salvato in Download: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                saveToExternalFiles(context, repository.exportCsvString(), "portale_finanziario_backup.csv")
            }
        }
    }

    private fun saveToExternalFiles(context: Context, content: String, filename: String) {
        try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, filename)
            file.writeText(content)
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "File salvato nella memoria locale dell'app!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Errore salvataggio locale: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvString = repository.exportCsvString()
                val file = java.io.File(context.cacheDir, "portale_finanziario_backup.csv")
                file.writeText(csvString)

                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Backup Portale Finanziario Familiare")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                launch(Dispatchers.Main) {
                    val chooser = android.content.Intent.createChooser(shareIntent, "Condividi / Salva Backup CSV")
                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nella condivisione: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun copyCsvToClipboard(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvString = repository.exportCsvString()
                launch(Dispatchers.Main) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Backup CSV Finanziario", csvString)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Dati CSV copiati negli appunti!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nella copia: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Impossibile aprire il file selezionato.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val csvContent = inputStream.bufferedReader().use { it.readText() }
                if (csvContent.isBlank()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Il file selezionato è vuoto.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val count = repository.importCsvString(csvContent)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Importazione completata con successo ($count elementi)!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nell'importazione: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importCsvContent(context: Context, csvContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (csvContent.isBlank()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Nessun testo da importare.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val count = repository.importCsvString(csvContent)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Importazione completata con successo ($count elementi)!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nell'importazione: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importFromDownloadedFile(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appDownloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val appFile = appDownloadDir?.let { java.io.File(it, "portale_finanziario_backup.csv") }

                @Suppress("DEPRECATION")
                val publicDownloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val publicFile = java.io.File(publicDownloadDir, "portale_finanziario_backup.csv")

                val fileToRead = when {
                    appFile != null && appFile.exists() && appFile.length() > 0 -> appFile
                    publicFile.exists() && publicFile.length() > 0 -> publicFile
                    else -> null
                }

                if (fileToRead != null) {
                    val content = fileToRead.readText()
                    val count = repository.importCsvString(content)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Importato da Download (${fileToRead.name}): $count elementi!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Nessun file 'portale_finanziario_backup.csv' trovato nella cartella Download.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Errore importazione locale: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatMonthLabel(ymKey: String): String {
        return try {
            val parts = ymKey.split("-")
            val month = parts[1]
            val yearShort = parts[0].takeLast(2)
            "$month/$yearShort"
        } catch (e: Exception) {
            ymKey
        }
    }
}
