package com.example.moneymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moneymanager.ui.viewmodel.MoneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var newCategory by remember { mutableStateOf("") }
    
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var editCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("New Category") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newCategory.isNotBlank()) {
                            viewModel.addCategory(newCategory.trim())
                            newCategory = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category)
                            Row {
                                IconButton(onClick = { 
                                    categoryToEdit = category 
                                    editCategoryName = category
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Category Dialog
        categoryToEdit?.let { oldName ->
            AlertDialog(
                onDismissRequest = { categoryToEdit = null },
                title = { Text("Edit Category") },
                text = {
                    Column {
                        Text("Renaming this category will also update all existing transactions associated with it.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editCategoryName,
                            onValueChange = { editCategoryName = it },
                            label = { Text("Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editCategoryName.isNotBlank() && editCategoryName != oldName) {
                                viewModel.renameCategory(oldName, editCategoryName.trim())
                            }
                            categoryToEdit = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToEdit = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
