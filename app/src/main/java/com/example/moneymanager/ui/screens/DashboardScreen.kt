package com.example.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.util.FormatUtils
import com.example.moneymanager.ui.theme.IncomeGreen
import com.example.moneymanager.ui.viewmodel.DailyTransactionGroup
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import com.example.moneymanager.ui.viewmodel.TransactionUiModel
import java.util.Locale

// Pre-allocated static shapes to eliminate runtime allocations during scrolling
private val BadgeCornerShape = RoundedCornerShape(6.dp)
private val SummaryCornerShape = RoundedCornerShape(14.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MoneyViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val monthlySpendings by viewModel.spendingsForSelectedMonth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncomeForSelectedMonth.collectAsState()
    val groupedTransactions by viewModel.groupedTransactionsForSelectedMonth.collectAsState()
    val currentCalendar by viewModel.currentCalendar.collectAsState()

    val locale = remember { Locale.getDefault() }

    // Stabilize navigation lambda
    val currentOnNavigateToEdit by rememberUpdatedState(onNavigateToEditTransaction)
    val onItemClick = remember { { id: Long -> currentOnNavigateToEdit(id) } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            // Compact Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = viewModel::previousMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = FormatUtils.formatShortMonthYear(currentCalendar.timeInMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = viewModel::nextMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }

            // Compact Balance Summary Bar
            BalanceSummaryBar(
                totalBalance = totalBalance,
                monthlyIncome = monthlyIncome,
                monthlySpendings = monthlySpendings,
                locale = locale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (groupedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions for this month.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp) // Compact card spacing
                ) {
                    groupedTransactions.forEach { group ->
                        item(
                            key = "header_${group.dateMillis}",
                            contentType = "day_header"
                        ) {
                            DayHeader(
                                group = group,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }

                        items(
                            items = group.transactions,
                            key = { it.id },
                            contentType = { "transaction_row" }
                        ) { itemModel ->
                            TransactionItem(
                                item = itemModel,
                                onClick = onItemClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    group: DailyTransactionGroup,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.dayOfMonth,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, BadgeCornerShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.monthYear,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                group.formattedDailyIncome?.let { incomeText ->
                    Text(
                        text = incomeText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
                group.formattedDailyExpense?.let { expenseText ->
                    Text(
                        text = expenseText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun BalanceSummaryBar(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlySpendings: Double,
    locale: Locale = Locale.getDefault(),
    modifier: Modifier = Modifier
) {
    val formattedIncome = remember(monthlyIncome, locale) { String.format(locale, "+€%.2f", monthlyIncome) }
    val formattedExpense = remember(monthlySpendings, locale) { String.format(locale, "-€%.2f", monthlySpendings) }
    val formattedBalance = remember(totalBalance, locale) { String.format(locale, "€%.2f", totalBalance) }

    Card(
        modifier = modifier,
        shape = SummaryCornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 8.dp, horizontal = 8.dp), // Compact padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMetricColumn(
                label = "Income",
                amount = formattedIncome,
                amountColor = IncomeGreen,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            SummaryMetricColumn(
                label = "Expenses",
                amount = formattedExpense,
                amountColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            SummaryMetricColumn(
                label = "Balance",
                amount = formattedBalance,
                amountColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}