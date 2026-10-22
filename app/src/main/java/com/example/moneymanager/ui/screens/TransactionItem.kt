package com.example.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.core.util.FormatUtils
import com.example.moneymanager.ui.theme.IncomeGreen
import com.example.moneymanager.ui.viewmodel.TransactionUiModel
import java.util.Locale

private val CardCornerShape = RoundedCornerShape(12.dp)
private val BadgeCornerShape = RoundedCornerShape(6.dp)

@Composable
fun TransactionItem(
    item: TransactionUiModel,
    onClick: (Long) -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)

    val amountColor = if (item.isIncome) IncomeGreen else MaterialTheme.colorScheme.error
    val iconBgColor = remember(amountColor) { amountColor.copy(alpha = 0.12f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { currentOnClick(item.id) }
            .padding(horizontal = 12.dp, vertical = 7.dp), // Compact row height (down from 10.dp)
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact Leading Indicator (reduced from 34.dp to 28.dp)
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(14.dp) // Reduced from 16.dp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name & Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount & Category Badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 110.dp)
                    .background(MaterialTheme.colorScheme.surface, BadgeCornerShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp) // Tightened badge padding
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: (Long) -> Unit
) {
    val locale = Locale.getDefault()

    val isInc = transaction.type == TransactionType.INCOME
    val timeStr = FormatUtils.formatTime(transaction.timestamp)
    val subtitle = if (transaction.description.isNotBlank()) {
        "$timeStr • ${transaction.description}"
    } else {
        timeStr
    }

    val uiModel = remember(transaction, isInc, subtitle) {
        TransactionUiModel(
            transaction = transaction,
            id = transaction.id,
            name = transaction.name,
            subtitle = subtitle,
            formattedAmount = String.format(locale, "%s€%.2f", if (isInc) "+" else "-", transaction.amount),
            category = transaction.category,
            isIncome = isInc
        )
    }

    TransactionItem(
        item = uiModel,
        onClick = onClick
    )
}