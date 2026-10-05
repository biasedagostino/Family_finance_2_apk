package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_details")
data class PersonalDetailEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val description: String,
    val amount: Double,
    val type: String, // "Spesa" or "Entrata"
    val category: String
)
