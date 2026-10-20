package com.example.moneymanager.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DailyTransactionGroup(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeek: String,
    val monthYear: String,
    val dailyIncome: Double,
    val dailyExpense: Double,
    val transactions: List<Transaction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MoneyViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val monthlySpendings by viewModel.spendingsForSelectedMonth.collectAsState()
    val transactions by viewModel.transactionsForSelectedMonth.collectAsState()
    val currentCalendar by viewModel.currentCalendar.collectAsState()

    val monthlyIncome = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    // Group transactions by day (newest first)
    val groupedTransactions = remember(transactions) {
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val dowFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val myFormat = SimpleDateFormat("MM.yyyy", Locale.getDefault())

        transactions
            .sortedByDescending { it.timestamp }
            .groupBy { transaction ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = transaction.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            .map { (dayStartMillis, dayTxList) ->
                val date = Date(dayStartMillis)
                DailyTransactionGroup(
                    dateMillis = dayStartMillis,
                    dayOfMonth = dayFormat.format(date),
                    dayOfWeek = dowFormat.format(date),
                    monthYear = myFormat.format(date),
                    dailyIncome = dayTxList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    dailyExpense = dayTxList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    transactions = dayTxList
                )
            }
    }

    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = monthFormat.format(currentCalendar.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            )
        },
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
                .padding(padding)
        ) {
            // Compact Single-Line Summary Bar
            BalanceSummaryBar(
                totalBalance = totalBalance,
                monthlyIncome = monthlyIncome,
                monthlySpendings = monthlySpendings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Transactions List Grouped by Date
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { group ->
                        item(key = "header_${group.dateMillis}") {
                            DayHeader(
                                group = group,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        items(
                            items = group.transactions,
                            key = { it.id }
                        ) { transaction ->
                            SwipeableTransactionItem(
                                transaction = transaction,
                                isPendingDelete = transactionToDelete?.id == transaction.id,
                                onClick = { onNavigateToEditTransaction(transaction.id) },
                                onSwipeToDelete = { transactionToDelete = transaction }
                            )
                        }
                    }
                }
            }
        }

        // Confirmation Dialog
        transactionToDelete?.let { transaction ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete \"${transaction.name}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(transaction)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
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
            // Left: Day Number, Day-of-Week Badge, Month.Year
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.dayOfMonth,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = group.dayOfWeek,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.monthYear,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Daily Income & Daily Expense Totals
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (group.dailyIncome > 0) {
                    Text(
                        text = String.format(Locale.getDefault(), "+€%.2f", group.dailyIncome),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
                if (group.dailyExpense > 0) {
                    Text(
                        text = String.format(Locale.getDefault(), "-€%.2f", group.dailyExpense),
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMetricColumn(
                label = "Income",
                amount = String.format(Locale.getDefault(), "+€%.2f", monthlyIncome),
                amountColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            SummaryMetricColumn(
                label = "Expenses",
                amount = String.format(Locale.getDefault(), "-€%.2f", monthlySpendings),
                amountColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            SummaryMetricColumn(
                label = "Balance",
                amount = String.format(Locale.getDefault(), "€%.2f", totalBalance),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    isPendingDelete: Boolean,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    // Only require swiping 25% of the card's width instead of the default 50%
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    // 1. Trigger the confirmation dialog once the swipe settles on EndToStart
    LaunchedEffect(dismissState.settledValue) {
        if (dismissState.settledValue == SwipeToDismissBoxValue.EndToStart) {
            onSwipeToDelete()
        }
    }

    // 2. When the user cancels the dialog (isPendingDelete becomes false), animate the card back
    LaunchedEffect(isPendingDelete) {
        if (!isPendingDelete && dismissState.settledValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                    dismissState.settledValue == SwipeToDismissBoxValue.EndToStart

            val backgroundColor by animateColorAsState(
                targetValue = if (isSwiping) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color.Transparent
                },
                label = "SwipeBackground"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        TransactionItemContent(
            transaction = transaction,
            onClick = onClick
        )
    }
}
@Composable
fun TransactionItemContent(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val iconBgColor = amountColor.copy(alpha = 0.12f)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            // Opaque color prevents the swipe background from bleeding through
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Leading Type Indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Left Side: Name + Time (and optional Description)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (transaction.description.isNotBlank()) {
                    "${timeFormat.format(Date(transaction.timestamp))} • ${transaction.description}"
                } else {
                    timeFormat.format(Date(transaction.timestamp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side: Amount & Category Badge Underneath
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%s€%.2f",
                        if (isIncome) "+" else "-",
                        transaction.amount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 110.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}