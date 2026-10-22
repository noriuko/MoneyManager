package com.example.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    transactionId: Long?
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedDate by remember { mutableLongStateOf(Calendar.getInstance().timeInMillis) }

    val categories by viewModel.categories.collectAsState()
    var expandedCategory by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Calculator Dialog State
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var adjustmentAmount by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Pre-fill data if editing
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.getTransactionById(transactionId)?.let { transaction ->
                name = transaction.name
                amount = transaction.amount.toString()
                category = transaction.category
                description = transaction.description
                type = transaction.type
                selectedDate = transaction.timestamp
            }
        }
    }

    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    val isFormValid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0.0 && category.isNotBlank()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transactionId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (isFormValid && parsedAmount != null) {
                            viewModel.addTransaction(
                                Transaction(
                                    id = transactionId ?: 0L,
                                    name = name.trim(),
                                    amount = parsedAmount,
                                    description = description.trim(),
                                    timestamp = selectedDate,
                                    category = category.trim(),
                                    type = type
                                )
                            )
                            onBack()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (transactionId == null) "Save Transaction" else "Update Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        if (showDeleteConfirm && transactionId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(Transaction(id = transactionId, name = "", amount = 0.0, description = "", timestamp = 0L, category = "", type = com.example.moneymanager.core.data.TransactionType.EXPENSE))
                            showDeleteConfirm = false
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Transaction Type Selector
            TransactionTypeSelector(
                selectedType = type,
                onTypeSelected = { type = it }
            )

            // Amount Field
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                        amount = input
                    }
                },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "€ ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailingIcon = {
                    if (transactionId != null) {
                        IconButton(onClick = { showCalculatorDialog = true }) {
                            Icon(Icons.Default.Calculate, contentDescription = "Adjust Amount")
                        }
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title / Name") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                if (categories.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }

            // Separate Date & Time Selector Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedCard(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Select Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = timeFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Date Picker Modal
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { pickedMillis ->
                                val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val newCal = Calendar.getInstance().apply { timeInMillis = pickedMillis }
                                newCal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                newCal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                selectedDate = newCal.timeInMillis
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time Picker Modal
        if (showTimePicker) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val timePickerState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            newCal.set(Calendar.MINUTE, timePickerState.minute)
                            selectedDate = newCal.timeInMillis
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }

        // Calculator Modal
        if (showCalculatorDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCalculatorDialog = false 
                    adjustmentAmount = ""
                },
                title = { Text("Adjust Amount") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add or subtract from the current amount.")
                        OutlinedTextField(
                            value = adjustmentAmount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    adjustmentAmount = input
                                }
                            },
                            label = { Text("Adjustment") },
                            prefix = { Text("€ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentAmt = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val adjustAmt = adjustmentAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val newAmt = currentAmt + adjustAmt
                            amount = if (newAmt < 0) "0.0" else String.format(Locale.US, "%.2f", newAmt)
                            showCalculatorDialog = false
                            adjustmentAmount = ""
                        }
                    ) {
                        Text("Add (+)")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val currentAmt = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val adjustAmt = adjustmentAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val newAmt = currentAmt - adjustAmt
                            amount = if (newAmt < 0) "0.0" else String.format(Locale.US, "%.2f", newAmt)
                            showCalculatorDialog = false
                            adjustmentAmount = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Subtract (-)")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isExpense = selectedType == TransactionType.EXPENSE
            val expenseBg = if (isExpense) MaterialTheme.colorScheme.errorContainer else Color.Transparent
            val expenseContent = if (isExpense) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

            val isIncome = selectedType == TransactionType.INCOME
            val incomeBg = if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color.Transparent
            val incomeContent = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant

            // Expense Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(expenseBg)
                    .clickable { onTypeSelected(TransactionType.EXPENSE) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = expenseContent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Expense",
                        fontWeight = FontWeight.Bold,
                        color = expenseContent
                    )
                }
            }

            // Income Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(incomeBg)
                    .clickable { onTypeSelected(TransactionType.INCOME) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = incomeContent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Income",
                        fontWeight = FontWeight.Bold,
                        color = incomeContent
                    )
                }
            }
        }
    }
}