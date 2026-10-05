package com.example.data.csv

import com.example.data.local.CategoryEntity
import com.example.data.local.PersonalDetailEntity
import com.example.data.local.SettingEntity
import com.example.data.local.TransactionEntity

data class ParsedCsvData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val personalDetails: List<PersonalDetailEntity>,
    val settings: List<SettingEntity>
)

object CsvEngine {

    fun parse(csvText: String): ParsedCsvData {
        val categories = mutableListOf<CategoryEntity>()
        val transactions = mutableListOf<TransactionEntity>()
        val personalDetails = mutableListOf<PersonalDetailEntity>()
        val settings = mutableListOf<SettingEntity>()

        var currentSection = ""

        val lines = csvText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("---") && trimmed.endsWith("---")) {
                currentSection = trimmed.replace("-", "").trim()
                continue
            }

            val tokens = parseCsvLine(trimmed)
            if (tokens.isEmpty()) continue

            // Skip headers
            if (tokens[0].equals("Nome", ignoreCase = true) ||
                tokens[0].equals("Data", ignoreCase = true) ||
                tokens[0].equals("Chiave", ignoreCase = true)
            ) {
                continue
            }

            when (currentSection) {
                "Categorie" -> {
                    if (tokens.size >= 2) {
                        val name = unquote(tokens[0])
                        val type = unquote(tokens[1])
                        categories.add(CategoryEntity(name = name, type = type))
                    }
                }
                "Transazioni" -> {
                    if (tokens.size >= 5) {
                        val date = unquote(tokens[0])
                        val desc = unquote(tokens[1])
                        val amount = unquote(tokens[2]).toDoubleOrNull() ?: 0.0
                        val type = unquote(tokens[3])
                        val category = unquote(tokens[4])
                        val isFuture = if (tokens.size >= 6) unquote(tokens[5]).toBoolean() else false
                        val recurrenceId = if (tokens.size >= 7) unquote(tokens[6]) else ""

                        transactions.add(
                            TransactionEntity(
                                date = date,
                                description = desc,
                                amount = amount,
                                type = type,
                                category = category,
                                isFuture = isFuture,
                                recurrenceId = recurrenceId
                            )
                        )
                    }
                }
                "Dettagli Personali" -> {
                    if (tokens.size >= 5) {
                        val date = unquote(tokens[0])
                        val desc = unquote(tokens[1])
                        val amount = unquote(tokens[2]).toDoubleOrNull() ?: 0.0
                        val type = unquote(tokens[3])
                        val category = unquote(tokens[4])

                        personalDetails.add(
                            PersonalDetailEntity(
                                date = date,
                                description = desc,
                                amount = amount,
                                type = type,
                                category = category
                            )
                        )
                    }
                }
                "Impostazioni" -> {
                    if (tokens.size >= 2) {
                        val key = unquote(tokens[0])
                        val value = unquote(tokens[1])
                        settings.add(SettingEntity(key = key, value = value))
                    }
                }
            }
        }

        // Apply settings flags to categories if available
        val savingsCatSetting = settings.find { it.key.lowercase().contains("risparmio") && it.key.lowercase().contains("categorie") }?.value ?: ""
        val savingsCats = savingsCatSetting.split("|").map { it.trim() }.toSet()

        val personalBudgetCatSetting = settings.find { it.key.lowercase().contains("budget personale") }?.value ?: ""

        val updatedCategories = categories.map { cat ->
            cat.copy(
                useForSavings = savingsCats.contains(cat.name),
                useForPersonalBudget = (cat.name.equals(personalBudgetCatSetting, ignoreCase = true))
            )
        }

        return ParsedCsvData(
            categories = updatedCategories,
            transactions = transactions,
            personalDetails = personalDetails,
            settings = settings
        )
    }

    fun build(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        personalDetails: List<PersonalDetailEntity>,
        settings: List<SettingEntity>
    ): String {
        val sb = StringBuilder()

        // Section 1: Categorie
        sb.append("--- Categorie ---\n")
        sb.append("Nome,Tipo\n")
        for (cat in categories) {
            sb.append("\"${cat.name}\",\"${cat.type}\"\n")
        }
        sb.append("\n")

        // Section 2: Transazioni
        sb.append("--- Transazioni ---\n")
        sb.append("Data,Descrizione,Importo,Tipo,Categoria,Futura,RecurrenceId\n")
        for (t in transactions) {
            val amountStr = if (t.amount % 1.0 == 0.0) t.amount.toInt().toString() else t.amount.toString()
            sb.append("\"${t.date}\",\"${t.description}\",$amountStr,\"${t.type}\",\"${t.category}\",${t.isFuture},\"${t.recurrenceId}\"\n")
        }
        sb.append("\n")

        // Section 3: Dettagli Personali
        sb.append("--- Dettagli Personali ---\n")
        sb.append("Data,Descrizione,Importo,Tipo,Categoria\n")
        for (pd in personalDetails) {
            val amountStr = if (pd.amount % 1.0 == 0.0) pd.amount.toInt().toString() else pd.amount.toString()
            sb.append("\"${pd.date}\",\"${pd.description}\",$amountStr,\"${pd.type}\",\"${pd.category}\"\n")
        }
        sb.append("\n")

        // Section 4: Impostazioni
        sb.append("--- Impostazioni ---\n")
        sb.append("Chiave,Valore\n")
        for (s in settings) {
            val valOut = if (s.value.contains("|") || s.value.contains(" ")) "\"${s.value}\"" else s.value
            sb.append("\"${s.key}\",$valOut\n")
        }

        return sb.toString()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun unquote(str: String): String {
        var s = str.trim()
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1)
        }
        return s
    }
}
