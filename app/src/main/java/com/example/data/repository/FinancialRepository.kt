package com.example.data.repository

import android.content.Context
import com.example.data.csv.CsvEngine
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class FinancialRepository(private val db: AppDatabase) {

    val categories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val personalDetails: Flow<List<PersonalDetailEntity>> = db.personalDetailDao().getAllPersonalDetails()
    val settings: Flow<List<SettingEntity>> = db.settingDao().getAllSettings()

    suspend fun initializeDefaultDataIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val existingCats = db.categoryDao().getAllCategories().first()
        if (existingCats.isEmpty()) {
            try {
                val csvContent = context.assets.open("initial_data.csv").bufferedReader().use { it.readText() }
                val parsed = CsvEngine.parse(csvContent)

                db.categoryDao().insertAll(parsed.categories)
                db.transactionDao().insertAll(parsed.transactions)
                db.personalDetailDao().insertAll(parsed.personalDetails)
                db.settingDao().insertAll(parsed.settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun exportCsvString(): String = withContext(Dispatchers.IO) {
        val cats = db.categoryDao().getAllCategories().first()
        val txs = db.transactionDao().getAllTransactions().first()
        val pds = db.personalDetailDao().getAllPersonalDetails().first()
        val sts = db.settingDao().getAllSettings().first()
        CsvEngine.build(cats, txs, pds, sts)
    }

    suspend fun importCsvString(csvContent: String) = withContext(Dispatchers.IO) {
        val parsed = CsvEngine.parse(csvContent)
        db.categoryDao().deleteAll()
        db.transactionDao().deleteAll()
        db.personalDetailDao().deleteAll()
        db.settingDao().deleteAll()

        db.categoryDao().insertAll(parsed.categories)
        db.transactionDao().insertAll(parsed.transactions)
        db.personalDetailDao().insertAll(parsed.personalDetails)
        db.settingDao().insertAll(parsed.settings)
    }

    suspend fun addCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategory(category)
    }

    suspend fun deleteCategory(name: String) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategoryByName(name)
    }

    suspend fun setPersonalBudgetCategory(name: String) = withContext(Dispatchers.IO) {
        db.categoryDao().resetPersonalBudgetCategory()
        db.categoryDao().setPersonalBudgetCategory(name)
        db.settingDao().insertSetting(SettingEntity("Categoria Budget Personale", name))
    }

    suspend fun toggleSavingsCategory(categoryName: String, isSavings: Boolean) = withContext(Dispatchers.IO) {
        val currentCats = db.categoryDao().getAllCategories().first()
        val updated = currentCats.map { if (it.name == categoryName) it.copy(useForSavings = isSavings) else it }
        db.categoryDao().insertAll(updated)

        // Update settings string
        val savingsCatNames = updated.filter { it.useForSavings }.joinToString("|") { it.name }
        db.settingDao().insertSetting(SettingEntity("Categorie Risparmio", savingsCatNames))
    }

    suspend fun addTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        db.transactionDao().insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) = withContext(Dispatchers.IO) {
        db.transactionDao().deleteTransactionById(id)
    }

    suspend fun addPersonalDetail(personalDetail: PersonalDetailEntity) = withContext(Dispatchers.IO) {
        db.personalDetailDao().insertPersonalDetail(personalDetail)
    }

    suspend fun deletePersonalDetail(id: Int) = withContext(Dispatchers.IO) {
        db.personalDetailDao().deletePersonalDetailById(id)
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db.settingDao().insertSetting(SettingEntity(key, value))
    }
}
