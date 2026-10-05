package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)

    fun isoToDisplay(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate.trim()) ?: return isoDate
            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun displayToIso(displayDate: String): String {
        return try {
            val date = displayFormat.parse(displayDate.trim()) ?: return displayDate
            isoFormat.format(date)
        } catch (e: Exception) {
            displayDate
        }
    }

    fun todayDisplay(): String {
        return displayFormat.format(Date())
    }

    fun todayIso(): String {
        return isoFormat.format(Date())
    }

    fun formatMillisToDisplay(millis: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        return displayFormat.format(calendar.time)
    }

    fun formatMillisToIso(millis: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        return isoFormat.format(calendar.time)
    }

    fun parseToCalendar(dateStr: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            if (dateStr.contains("/")) {
                cal.time = displayFormat.parse(dateStr) ?: Date()
            } else if (dateStr.contains("-")) {
                cal.time = isoFormat.parse(dateStr) ?: Date()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cal
    }

    fun calculateLastRepetitionDate(startDateStr: String, frequency: String, repetitions: Int): String {
        val cal = parseToCalendar(startDateStr)
        val count = (repetitions - 1).coerceAtLeast(0)
        when (frequency.lowercase()) {
            "settimanale" -> cal.add(Calendar.WEEK_OF_YEAR, count)
            "annuale" -> cal.add(Calendar.YEAR, count)
            else -> cal.add(Calendar.MONTH, count) // "mensile"
        }
        return displayFormat.format(cal.time)
    }

    fun generateRecurringDates(startDateStr: String, frequency: String, repetitions: Int, dayRef: Int): List<String> {
        val dates = mutableListOf<String>()
        val cal = parseToCalendar(startDateStr)

        for (i in 0 until repetitions) {
            if (i > 0) {
                when (frequency.lowercase()) {
                    "settimanale" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "annuale" -> cal.add(Calendar.YEAR, 1)
                    else -> {
                        cal.add(Calendar.MONTH, 1)
                        if (dayRef > 0) {
                            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            cal.set(Calendar.DAY_OF_MONTH, dayRef.coerceAtMost(maxDay))
                        }
                    }
                }
            } else {
                if (dayRef > 0 && frequency.equals("mensile", ignoreCase = true)) {
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayRef.coerceAtMost(maxDay))
                }
            }
            dates.add(isoFormat.format(cal.time))
        }
        return dates
    }
}
