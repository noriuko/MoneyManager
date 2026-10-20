package com.example.moneymanager.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    // Determine all unique months that have transactions
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthsMap = remember(allTransactions) {
        val map = mutableMapOf<String, List<Transaction>>()
        allTransactions.forEach { tx ->
            val monthKey = monthFormat.format(Date(tx.timestamp))
            val list = map.getOrPut(monthKey) { mutableListOf() }
            (list as MutableList).add(tx)
        }
        // Sort months in descending order based on the first transaction's timestamp in that month
        map.toList().sortedByDescending { it.second.first().timestamp }
    }

    val selectedMonths = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val transactionsToExport = monthsMap
                            .filter { it.first in selectedMonths }
                            .flatMap { it.second }
                            .sortedBy { it.timestamp }
                        
                        val csvData = generateCsv(transactionsToExport)
                        
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csvData)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Export Transactions")
                        context.startActivity(shareIntent)
                    },
                    enabled = selectedMonths.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
                ) {
                    Text("Export to CSV", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Select months to export:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (monthsMap.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions available to export.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthsMap) { (monthKey, txList) ->
                        val isSelected = selectedMonths.contains(monthKey)
                        Card(
                            onClick = {
                                if (isSelected) {
                                    selectedMonths.remove(monthKey)
                                } else {
                                    selectedMonths.add(monthKey)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null // Handled by Card onClick
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = monthKey, fontWeight = FontWeight.Bold)
                                    Text(text = "${txList.size} transactions", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateCsv(transactions: List<Transaction>): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val builder = StringBuilder()
    
    for (tx in transactions) {
        val dateStr = dateFormat.format(Date(tx.timestamp))
        // Format: Name, dd/MM/yyyy HH:mm, Amount, Type, Category
        // Wrap strings that might contain commas in quotes to prevent CSV breaking
        val name = if (tx.name.contains(",")) "\"${tx.name}\"" else tx.name
        val category = if (tx.category.contains(",")) "\"${tx.category}\"" else tx.category
        
        builder.append("$name, $dateStr, ${tx.amount}, ${tx.type.name}, $category\n")
    }
    
    return builder.toString()
}
