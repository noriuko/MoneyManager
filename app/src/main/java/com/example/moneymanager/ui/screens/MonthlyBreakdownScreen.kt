package com.example.moneymanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.core.util.FormatUtils
import com.example.moneymanager.ui.theme.IncomeGreen
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.util.Locale

val categoryColors = listOf(
    Color(0xFFE57373), // Red
    Color(0xFF64B5F6), // Blue
    Color(0xFF81C784), // Green
    Color(0xFFFFB74D), // Orange
    Color(0xFFBA68C8), // Purple
    Color(0xFF4DB6AC), // Teal
    Color(0xFFFFD54F), // Yellow
    Color(0xFFAED581), // Light Green
    Color(0xFF7986CB), // Indigo
    Color(0xFF4DD0E1), // Cyan
    Color(0xFFF06292), // Pink
    Color(0xFFA1887F)  // Brown
)

private val CardCornerShape = RoundedCornerShape(12.dp)
private val IndicatorCornerShape = RoundedCornerShape(3.dp)

@Composable
fun MonthlyBreakdownScreen(
    viewModel: MoneyViewModel,
    onNavigateToCategoryDetails: (String, Boolean) -> Unit
) {
    val currentCalendar by viewModel.currentCalendar.collectAsState()
    val transactions by viewModel.transactionsForSelectedMonth.collectAsState()
    val locale = remember { Locale.getDefault() }

    var isIncomeTab by rememberSaveable { mutableStateOf(false) }

    val categoryBreakdown = remember(transactions, isIncomeTab) {
        val targetType = if (isIncomeTab) TransactionType.INCOME else TransactionType.EXPENSE
        transactions.filter { it.type == targetType }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalAmount = remember(categoryBreakdown) {
        categoryBreakdown.sumOf { it.second }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .statusBarsPadding() // Snaps content immediately below the status bar
        ) {
            // Compact Header: Month Navigation + Screen Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nested Row containing the month selector to match DashboardScreen exactly
                Row(
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

                Text(
                    text = "Monthly Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Income / Expense Toggle
            PrimaryTabRow(
                selectedTabIndex = if (isIncomeTab) 1 else 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !isIncomeTab,
                    onClick = { isIncomeTab = false },
                    text = { Text("Spendings") }
                )
                Tab(
                    selected = isIncomeTab,
                    onClick = { isIncomeTab = true },
                    text = { Text("Income") }
                )
            }

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isIncomeTab) "No income for this month." else "No expenses for this month.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val currentOnNavigateToCategory by rememberUpdatedState(onNavigateToCategoryDetails)
                val onItemClick = remember(isIncomeTab) { { cat: String -> currentOnNavigateToCategory(cat, isIncomeTab) } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Donut Chart Header Item
                    item(key = "chart_header", contentType = "chart") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                data = categoryBreakdown,
                                total = totalAmount,
                                colors = categoryColors,
                                modifier = Modifier.size(200.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isIncomeTab) "Total Income" else "Total Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(locale, "€%.2f", totalAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncomeTab) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Section Title
                    item(key = "categories_title", contentType = "title") {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    // Category List
                    itemsIndexed(
                        items = categoryBreakdown,
                        key = { _, item -> item.first },
                        contentType = { _, _ -> "category_card" }
                    ) { index, (category, amount) ->
                        val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                        val color = categoryColors[index % categoryColors.size]

                        CategoryCard(
                            category = category,
                            amount = amount,
                            percentage = percentage,
                            color = color,
                            isIncome = isIncomeTab,
                            locale = locale,
                            onClick = { onItemClick(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: String,
    amount: Double,
    percentage: Float,
    color: Color,
    isIncome: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardCornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp) // Tightened compact padding
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = String.format(locale, "%s€%.2f", if (isIncome) "+" else "-", amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(IndicatorCornerShape),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = String.format(locale, "%.1f%%", percentage * 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    data: List<Pair<String, Double>>,
    total: Double,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 24.dp
) {
    val animationProgress = remember(data) { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
    }

    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        val gapAngle = if (data.size > 1) 2.5f else 0f

        data.forEachIndexed { index, pair ->
            val rawSweep = if (total > 0) ((pair.second / total) * 360f).toFloat() else 0f
            val sweepAngle = (rawSweep - gapAngle).coerceAtLeast(0.5f) * animationProgress.value

            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle + (gapAngle / 2f),
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )

            startAngle += rawSweep * animationProgress.value
        }
    }
}