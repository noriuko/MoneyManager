package com.example.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moneymanager.ui.screens.AddScheduledTransactionScreen
import com.example.moneymanager.ui.screens.AddTransactionScreen
import com.example.moneymanager.ui.screens.BulkAddScreen
import com.example.moneymanager.ui.screens.CategoryManagementScreen
import com.example.moneymanager.ui.screens.DashboardScreen
import com.example.moneymanager.ui.screens.ExportScreen
import com.example.moneymanager.ui.screens.MonthlyBreakdownScreen
import com.example.moneymanager.ui.screens.ScheduledTransactionsScreen
import com.example.moneymanager.ui.screens.SettingsScreen
import com.example.moneymanager.ui.theme.MoneyManagerTheme
import com.example.moneymanager.ui.viewmodel.MoneyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val viewModel: MoneyViewModel = hiltViewModel()

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    val bottomNavRoutes = listOf("dashboard", "monthly", "settings")
                    val showBottomNav = currentRoute in bottomNavRoutes

                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (showBottomNav) {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        selected = currentRoute == "dashboard",
                                        onClick = { navController.navigate("dashboard") { launchSingleTop = true } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Monthly") },
                                        label = { Text("Monthly") },
                                        selected = currentRoute == "monthly",
                                        onClick = { navController.navigate("monthly") { launchSingleTop = true } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        selected = currentRoute == "settings",
                                        onClick = { navController.navigate("settings") { launchSingleTop = true } }
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController, 
                            startDestination = "dashboard",
                            modifier = Modifier
                                .padding(bottom = paddingValues.calculateBottomPadding())
                                .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddTransaction = { navController.navigate("add") },
                                    onNavigateToEditTransaction = { id -> navController.navigate("edit/$id") }
                                )
                            }
                            composable("monthly") {
                                MonthlyBreakdownScreen(
                                    viewModel = viewModel
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onNavigateToBulkAdd = { navController.navigate("bulk") },
                                    onNavigateToCategories = { navController.navigate("categories") },
                                    onNavigateToScheduled = { navController.navigate("scheduled") },
                                    onNavigateToExport = { navController.navigate("export") }
                                )
                            }
                            composable("export") {
                                ExportScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("scheduled") {
                                ScheduledTransactionsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToAddScheduled = { navController.navigate("add_scheduled") },
                                    onNavigateToEditScheduled = { id -> navController.navigate("edit_scheduled/$id") }
                                )
                            }
                            composable("add_scheduled") {
                                AddScheduledTransactionScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    scheduledId = null
                                )
                            }
                            composable("edit_scheduled/{scheduledId}") { backStackEntry ->
                                val scheduledIdStr = backStackEntry.arguments?.getString("scheduledId")
                                val scheduledId = scheduledIdStr?.toLongOrNull()
                                AddScheduledTransactionScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    scheduledId = scheduledId
                                )
                            }
                            composable("add") {
                                AddTransactionScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    transactionId = null // For new transactions
                                )
                            }
                            composable("edit/{transactionId}") { backStackEntry ->
                                val transactionIdStr = backStackEntry.arguments?.getString("transactionId")
                                val transactionId = transactionIdStr?.toLongOrNull()
                                AddTransactionScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    transactionId = transactionId
                                )
                            }
                            composable("bulk") {
                                BulkAddScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("categories") {
                                CategoryManagementScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
