package com.example.moneymanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowForward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBulkAdd: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToScheduled: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SettingsItem(
                title = "Manage Categories",
                onClick = onNavigateToCategories
            )
            HorizontalDivider()
            SettingsItem(
                title = "Bulk Add Transactions",
                onClick = onNavigateToBulkAdd
            )
            HorizontalDivider()
            SettingsItem(
                title = "Scheduled Transactions",
                onClick = onNavigateToScheduled
            )
            HorizontalDivider()
            SettingsItem(
                title = "Export to Text/CSV",
                onClick = onNavigateToExport
            )
            HorizontalDivider()
            SettingsItem(
                title = "Currency Selection (Placeholder)",
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
fun SettingsItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
