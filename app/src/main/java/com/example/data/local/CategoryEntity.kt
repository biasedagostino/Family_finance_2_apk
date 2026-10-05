package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val type: String, // "Spesa" or "Entrata"
    val useForPersonalBudget: Boolean = false,
    val useForSavings: Boolean = false
)
