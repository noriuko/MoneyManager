package com.example.moneymanager.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_transactions")
data class ScheduledTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val description: String,
    val dayOfMonth: Int, // 1 to 31
    val category: String,
    val type: TransactionType,
    var lastProcessedTimestamp: Long
)
