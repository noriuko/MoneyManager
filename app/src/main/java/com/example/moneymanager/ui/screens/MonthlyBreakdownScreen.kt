package com.example.moneymanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBreakdownScreen(viewModel: MoneyViewModel) {
    val currentCalendar by viewModel.currentCalendar.collectAsState()
    val rawCategoryBreakdown by viewModel.categoryBreakdownForSelectedMonth.collectAsState()
    val totalSpendings by viewModel.spendingsForSelectedMonth.collectAsState()

    // Sort descending by amount so highest expenses are always first
    val categoryBreakdown = remember(rawCategoryBreakdown) {
        rawCategoryBreakdown.sortedByDescending { it.second }
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Breakdown") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Navigation
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = monthFormat.format(currentCalendar.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses for this month.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chart Header Item
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                data = categoryBreakdown,
                                total = totalSpendings,
                                colors = categoryColors,
                                modifier = Modifier.size(220.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "€%.2f", totalSpendings),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Category List
                    itemsIndexed(
                        items = categoryBreakdown,
                        key = { _, item -> item.first }
                    ) { index, (category, amount) ->
                        val percentage = if (totalSpendings > 0) (amount / totalSpendings).toFloat() else 0f
                        val color = categoryColors[index % categoryColors.size]

                        CategoryCard(
                            category = category,
                            amount = amount,
                            percentage = percentage,
                            color = color
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
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = String.format(Locale.getDefault(), "€%.2f", amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", percentage * 100),
                    style = MaterialTheme.typography.labelMedium,
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
    strokeWidth: Dp = 28.dp
) {
    val animationProgress = remember(data) { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
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
        // Leave a tiny gap between slices if there is more than 1 category
        val gapAngle = if (data.size > 1) 2f else 0f

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