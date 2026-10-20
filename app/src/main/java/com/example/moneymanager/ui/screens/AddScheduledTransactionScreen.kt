package com.example.moneymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.data.ScheduledTransaction
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.ui.viewmodel.MoneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledTransactionScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    scheduledId: Long?
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var dayOfMonth by remember { mutableStateOf("1") }
    var lastProcessedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val categories by viewModel.categories.collectAsState()
    var expandedCategory by remember { mutableStateOf(false) }

    LaunchedEffect(scheduledId) {
        if (scheduledId != null) {
            viewModel.getScheduledTransactionById(scheduledId)?.let { st ->
                name = st.name
                amount = st.amount.toString()
                category = st.category
                description = st.description
                type = st.type
                dayOfMonth = st.dayOfMonth.toString()
                lastProcessedTimestamp = st.lastProcessedTimestamp
            }
        }
    }

    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    val parsedDay = dayOfMonth.toIntOrNull()
    val isFormValid = name.isNotBlank() && 
                      parsedAmount != null && parsedAmount > 0.0 && 
                      category.isNotBlank() &&
                      parsedDay != null && parsedDay in 1..31

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scheduledId == null) "Schedule Transaction" else "Edit Scheduled Transaction") },
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
                        if (isFormValid && parsedAmount != null && parsedDay != null) {
                            viewModel.addScheduledTransaction(
                                ScheduledTransaction(
                                    id = scheduledId ?: 0L,
                                    name = name.trim(),
                                    amount = parsedAmount,
                                    description = description.trim(),
                                    dayOfMonth = parsedDay,
                                    category = category.trim(),
                                    type = type,
                                    lastProcessedTimestamp = lastProcessedTimestamp
                                )
                            )
                            onBack()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (scheduledId == null) "Save" else "Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransactionTypeSelector(
                selectedType = type,
                onTypeSelected = { type = it }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) amount = input },
                label = { Text("Amount") },
                prefix = { Text("€ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title / Name") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (categories.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; expandedCategory = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = dayOfMonth,
                onValueChange = { input -> if (input.all { it.isDigit() }) dayOfMonth = input },
                label = { Text("Day of Month (1-31)") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                isError = dayOfMonth.isNotEmpty() && (dayOfMonth.toIntOrNull() == null || dayOfMonth.toInt() !in 1..31)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
