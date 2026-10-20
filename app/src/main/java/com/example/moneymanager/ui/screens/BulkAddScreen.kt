package com.example.moneymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moneymanager.ui.viewmodel.MoneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit
) {
    var bulkData by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Add Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Enter data in comma-separated format:\n" +
                "Name, dd/MM/yyyy HH:mm, Amount, Type, Category\n\n" +
                "Example:\n" +
                "Grocery, 25/12/2023 14:30, 50.0, EXPENSE, Food\n" +
                "Salary, 01/01/2024 09:00, 2000.0, INCOME, Job\n\n" +
                "(One transaction per line)",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = bulkData,
                onValueChange = { bulkData = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Bulk Data") },
                placeholder = {
                    Text("Grocery, 25/12/2023 14:30, 50.0, EXPENSE, Food\nSalary, 01/01/2024 09:00, 2000.0, INCOME, Job")
                }
            )

            Button(
                onClick = {
                    if (bulkData.isNotBlank()) {
                        viewModel.addBulkTransactions(bulkData)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Bulk Data")
            }
        }
    }
}
