package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val description: String,
    val amount: Double,
    val type: String, // "Spesa" or "Entrata"
    val category: String,
    val isFuture: Boolean = false,
    val recurrenceId: String = ""
)
