package com.example.moneymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    viewModel: MoneyViewModel,
    categoryName: String,
    isIncome: Boolean,
    onBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val transactions by viewModel.transactionsForSelectedMonth.collectAsState()
    
    val categoryTransactions = remember(transactions, categoryName, isIncome) {
        val targetType = if (isIncome) com.example.moneymanager.core.data.TransactionType.INCOME else com.example.moneymanager.core.data.TransactionType.EXPENSE
        transactions.filter { it.category == categoryName && it.type == targetType }.sortedByDescending { it.timestamp }
    }
    
    val currentCalendar by viewModel.currentCalendar.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(categoryName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = com.example.moneymanager.core.util.FormatUtils.formatFullMonthYear(currentCalendar.timeInMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (categoryTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions for $categoryName this month.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val currentOnNavigateToEdit by rememberUpdatedState(onNavigateToEditTransaction)
                val onItemClick = remember { { id: Long -> currentOnNavigateToEdit(id) } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = categoryTransactions,
                        key = { it.id },
                        contentType = { "transaction" }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}
