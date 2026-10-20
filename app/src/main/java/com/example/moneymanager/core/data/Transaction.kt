package com.example.moneymanager.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val description: String,
    val timestamp: Long,
    val category: String,
    val type: TransactionType
)
