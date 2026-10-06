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

        // Remove UTF-8 BOM if present and normalize line breaks
        val cleanText = csvText.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n").trim()
        if (cleanText.isEmpty()) {
            return ParsedCsvData(emptyList(), emptyList(), emptyList(), emptyList())
        }

        var currentSection = ""
        // If file contains no section markers, assume it's a transaction list
        val upperAll = cleanText.uppercase()
        if (!upperAll.contains("CATEGOR") && !upperAll.contains("---") && !upperAll.contains("[TRANSAZIONI]")) {
            currentSection = "Transazioni"
        }

        val lines = cleanText.lines()
        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Detect section headers in various formats: --- Section ---, [Section], or plain Section
            val lineUpper = trimmed.uppercase().replace("-", "").replace("[", "").replace("]", "").trim()
            if (lineUpper == "CATEGORIE" || lineUpper.startsWith("CATEGOR")) {
                currentSection = "Categorie"
                continue
            } else if (lineUpper == "TRANSAZIONI" || lineUpper.startsWith("TRANSAZ")) {
                currentSection = "Transazioni"
                continue
            } else if (lineUpper.contains("DETTAGLI") || lineUpper.contains("PERSONAL")) {
                currentSection = "Dettagli Personali"
                continue
            } else if (lineUpper.contains("IMPOSTAZION") || lineUpper.contains("SETTING")) {
                currentSection = "Impostazioni"
                continue
            }

            val delimiter = detectDelimiter(trimmed)
            val tokens = parseCsvLine(trimmed, delimiter)
            if (tokens.isEmpty()) continue

            // Skip table header rows
            val firstToken = unquote(tokens[0]).trim()
            if (firstToken.equals("Nome", ignoreCase = true) ||
                firstToken.equals("Data", ignoreCase = true) ||
                firstToken.equals("Date", ignoreCase = true) ||
                firstToken.equals("Chiave", ignoreCase = true) ||
                firstToken.equals("Key", ignoreCase = true)
            ) {
                continue
            }

            when (currentSection) {
                "Categorie" -> {
                    if (tokens.size >= 2) {
                        val name = unquote(tokens[0]).trim()
                        val type = unquote(tokens[1]).trim()
                        if (name.isNotEmpty()) {
                            categories.add(CategoryEntity(name = name, type = if (type.equals("Entrata", ignoreCase = true)) "Entrata" else "Spesa"))
                        }
                    } else if (tokens.size == 1) {
                        val name = unquote(tokens[0]).trim()
                        if (name.isNotEmpty()) {
                            categories.add(CategoryEntity(name = name, type = "Spesa"))
                        }
                    }
                }
                "Transazioni" -> {
                    if (tokens.size >= 3) {
                        val date = normalizeDate(tokens[0])
                        val desc = unquote(tokens[1]).trim()
                        val amount = parseAmount(tokens[2])
                        val type = if (tokens.size >= 4) {
                            val rawType = unquote(tokens[3]).trim()
                            if (rawType.equals("Entrata", ignoreCase = true) || rawType.equals("In", ignoreCase = true) || rawType == "+") "Entrata" else "Spesa"
                        } else "Spesa"
                        val category = if (tokens.size >= 5) unquote(tokens[4]).trim() else "Varie"
                        val isFuture = if (tokens.size >= 6) {
                            val rawFut = unquote(tokens[5]).trim().lowercase()
                            rawFut == "true" || rawFut == "1" || rawFut == "vero" || rawFut == "si" || rawFut == "sì"
                        } else false
                        val recurrenceId = if (tokens.size >= 7) unquote(tokens[6]).trim() else ""

                        if (desc.isNotEmpty() || amount > 0.0) {
                            transactions.add(
                                TransactionEntity(
                                    date = date,
                                    description = desc.ifEmpty { "Transazione" },
                                    amount = amount,
                                    type = type,
                                    category = category.ifEmpty { "Varie" },
                                    isFuture = isFuture,
                                    recurrenceId = recurrenceId
                                )
                            )
                        }
                    }
                }
                "Dettagli Personali" -> {
                    if (tokens.size >= 3) {
                        val date = normalizeDate(tokens[0])
                        val desc = unquote(tokens[1]).trim()
                        val amount = parseAmount(tokens[2])
                        val type = if (tokens.size >= 4) unquote(tokens[3]).trim() else "Spesa"
                        val category = if (tokens.size >= 5) unquote(tokens[4]).trim() else "Varie"

                        if (desc.isNotEmpty() || amount > 0.0) {
                            personalDetails.add(
                                PersonalDetailEntity(
                                    date = date,
                                    description = desc.ifEmpty { "Dettaglio" },
                                    amount = amount,
                                    type = if (type.equals("Entrata", ignoreCase = true)) "Entrata" else "Spesa",
                                    category = category.ifEmpty { "Varie" }
                                )
                            )
                        }
                    }
                }
                "Impostazioni" -> {
                    if (tokens.size >= 2) {
                        val key = unquote(tokens[0]).trim()
                        val value = unquote(tokens[1]).trim()
                        if (key.isNotEmpty()) {
                            settings.add(SettingEntity(key = key, value = value))
                        }
                    }
                }
            }
        }

        // Apply settings flags to categories if available
        val savingsCatSetting = settings.find { it.key.lowercase().contains("risparmio") && it.key.lowercase().contains("categorie") }?.value ?: ""
        val savingsCats = savingsCatSetting.split("|").map { it.trim() }.toSet()

        val personalBudgetCatSetting = settings.find { it.key.lowercase().contains("budget personale") }?.value ?: ""

        // If categories were not listed in the file, automatically derive them from transactions
        val finalCategories = if (categories.isEmpty() && transactions.isNotEmpty()) {
            transactions.map { it.category }.distinct().map { catName ->
                val sampleTx = transactions.firstOrNull { it.category == catName }
                val catType = sampleTx?.type ?: "Spesa"
                CategoryEntity(
                    name = catName,
                    type = catType,
                    useForSavings = savingsCats.contains(catName),
                    useForPersonalBudget = (catName.equals(personalBudgetCatSetting, ignoreCase = true))
                )
            }
        } else {
            categories.map { cat ->
                cat.copy(
                    useForSavings = savingsCats.contains(cat.name),
                    useForPersonalBudget = (cat.name.equals(personalBudgetCatSetting, ignoreCase = true))
                )
            }
        }

        return ParsedCsvData(
            categories = finalCategories,
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
            sb.append("\"${escape(cat.name)}\",\"${escape(cat.type)}\"\n")
        }
        sb.append("\n")

        // Section 2: Transazioni
        sb.append("--- Transazioni ---\n")
        sb.append("Data,Descrizione,Importo,Tipo,Categoria,Futura,RecurrenceId\n")
        for (t in transactions) {
            val amountStr = if (t.amount % 1.0 == 0.0) t.amount.toInt().toString() else t.amount.toString()
            sb.append("\"${t.date}\",\"${escape(t.description)}\",$amountStr,\"${escape(t.type)}\",\"${escape(t.category)}\",${t.isFuture},\"${escape(t.recurrenceId)}\"\n")
        }
        sb.append("\n")

        // Section 3: Dettagli Personali
        sb.append("--- Dettagli Personali ---\n")
        sb.append("Data,Descrizione,Importo,Tipo,Categoria\n")
        for (pd in personalDetails) {
            val amountStr = if (pd.amount % 1.0 == 0.0) pd.amount.toInt().toString() else pd.amount.toString()
            sb.append("\"${pd.date}\",\"${escape(pd.description)}\",$amountStr,\"${escape(pd.type)}\",\"${escape(pd.category)}\"\n")
        }
        sb.append("\n")

        // Section 4: Impostazioni
        sb.append("--- Impostazioni ---\n")
        sb.append("Chiave,Valore\n")
        for (s in settings) {
            sb.append("\"${escape(s.key)}\",\"${escape(s.value)}\"\n")
        }

        return sb.toString()
    }

    private fun detectDelimiter(line: String): Char {
        var commas = 0
        var semicolons = 0
        var inQuotes = false
        for (c in line) {
            if (c == '"') inQuotes = !inQuotes
            else if (!inQuotes) {
                if (c == ',') commas++
                else if (c == ';') semicolons++
            }
        }
        return if (semicolons > commas) ';' else ','
    }

    private fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> {
                    // Check for escaped quote ""
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                    } else if (i > 0 && line[i - 1] == '"' && inQuotes) {
                        // second quote of pair already appended, do nothing
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
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
            s = s.substring(1, s.length - 1).replace("\"\"", "\"")
        }
        return s
    }

    private fun escape(str: String): String {
        return str.replace("\"", "\"\"")
    }

    private fun parseAmount(str: String): Double {
        val clean = unquote(str).trim()
            .replace("€", "")
            .replace("EUR", "")
            .replace(" ", "")
            .replace("\u00A0", "")
        if (clean.isEmpty()) return 0.0

        return try {
            if (clean.contains(".") && clean.contains(",")) {
                clean.replace(".", "").replace(",", ".").toDouble()
            } else if (clean.contains(",")) {
                clean.replace(",", ".").toDouble()
            } else {
                clean.toDouble()
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun normalizeDate(rawDate: String): String {
        val date = unquote(rawDate).trim()
        if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return date
        }
        if (date.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            val parts = date.split("/")
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        if (date.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
            val parts = date.split("-")
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        return date
    }
}
