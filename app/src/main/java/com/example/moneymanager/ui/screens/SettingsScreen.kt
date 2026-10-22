package com.example.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.repository.ThemeMode
import com.example.moneymanager.ui.viewmodel.MoneyViewModel

private val GridButtonShape = RoundedCornerShape(14.dp)

private data class SettingButtonItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsScreen(
    viewModel: MoneyViewModel,
    onNavigateToBulkAdd: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToScheduled: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeSubtitle = when (themeMode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    val settingItems = remember(themeSubtitle) {
        listOf(
            SettingButtonItem(
                title = "App Theme",
                subtitle = themeSubtitle,
                icon = Icons.Default.Palette,
                onClick = { showThemeDialog = true }
            ),
            SettingButtonItem(
                title = "Categories",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategories
            ),
            SettingButtonItem(
                title = "Bulk Add",
                icon = Icons.Default.PlaylistAdd,
                onClick = onNavigateToBulkAdd
            ),
            SettingButtonItem(
                title = "Scheduled",
                icon = Icons.Default.EventRepeat,
                onClick = onNavigateToScheduled
            ),
            SettingButtonItem(
                title = "Export CSV",
                icon = Icons.Default.IosShare,
                onClick = onNavigateToExport
            ),
            SettingButtonItem(
                title = "Currency",
                subtitle = "EUR (€)",
                icon = Icons.Default.CurrencyExchange,
                onClick = { /* TODO */ }
            )
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            // Compact Header matching Dashboard and MonthlyBreakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3-Per-Row Button Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(settingItems, key = { it.title }) { item ->
                    SettingsGridButton(item = item)
                }
            }
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentMode = themeMode,
                onSelectMode = { mode ->
                    viewModel.setThemeMode(mode)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsGridButton(
    item: SettingButtonItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(GridButtonShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = item.onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (item.subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOptionRow(
                    text = "System default",
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onSelectMode(ThemeMode.SYSTEM) }
                )
                ThemeOptionRow(
                    text = "Light",
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onSelectMode(ThemeMode.LIGHT) }
                )
                ThemeOptionRow(
                    text = "Dark",
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onSelectMode(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}