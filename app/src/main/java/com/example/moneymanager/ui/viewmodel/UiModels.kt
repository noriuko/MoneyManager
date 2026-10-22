package com.example.moneymanager.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.moneymanager.core.data.Transaction

@Immutable
data class TransactionUiModel(
    val transaction: Transaction,
    val id: Long,
    val name: String,
    val subtitle: String,
    val formattedAmount: String,
    val category: String,
    val isIncome: Boolean
)

@Immutable
data class DailyTransactionGroup(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeek: String,
    val monthYear: String,
    val formattedDailyIncome: String?,
    val formattedDailyExpense: String?,
    val transactions: List<TransactionUiModel>
)
