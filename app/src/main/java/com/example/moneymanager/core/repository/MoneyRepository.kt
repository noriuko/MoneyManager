package com.example.moneymanager.core.repository

import com.example.moneymanager.core.data.Category
import com.example.moneymanager.core.data.MoneyDao
import com.example.moneymanager.core.data.ScheduledTransaction
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> = moneyDao.getAllTransactions()

    fun getCategories(): Flow<List<String>> = moneyDao.getCategories()

    suspend fun insertTransaction(transaction: Transaction) {
        moneyDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        moneyDao.updateTransaction(transaction)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return moneyDao.getTransactionById(id)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return moneyDao.getTransactionsBetween(startDate, endDate)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        moneyDao.insertTransactions(transactions)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        moneyDao.deleteTransaction(transaction)
    }
    
    suspend fun insertCategory(category: String) {
        moneyDao.insertCategory(Category(category))
    }
    
    suspend fun deleteCategory(category: String) {
        moneyDao.deleteCategory(Category(category))
    }

    @androidx.room.Transaction
    suspend fun renameCategory(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return
        // Insert the new category
        moneyDao.insertCategory(Category(newName))
        // Update all associated transactions
        moneyDao.updateTransactionsCategory(oldName = oldName, newName = newName)
        moneyDao.updateScheduledTransactionsCategory(oldName = oldName, newName = newName)
        // Delete the old category
        moneyDao.deleteCategory(Category(oldName))
    }

    // --- Scheduled Transactions ---

    fun getAllScheduledTransactions(): Flow<List<ScheduledTransaction>> = moneyDao.getAllScheduledTransactions()

    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction? {
        return moneyDao.getScheduledTransactionById(id)
    }

    suspend fun insertScheduledTransaction(st: ScheduledTransaction) {
        moneyDao.insertScheduledTransaction(st)
    }

    suspend fun updateScheduledTransaction(st: ScheduledTransaction) {
        moneyDao.updateScheduledTransaction(st)
    }

    suspend fun deleteScheduledTransaction(st: ScheduledTransaction) {
        moneyDao.deleteScheduledTransaction(st)
    }

    /**
     * Checks scheduled transactions and inserts new standard transactions for occurrences that have passed.
     */
    suspend fun processScheduledTransactions() {
        val scheduledTxs = moneyDao.getAllScheduledTransactionsSync()
        val today = Calendar.getInstance()

        for (st in scheduledTxs) {
            val lastProc = Calendar.getInstance().apply { timeInMillis = st.lastProcessedTimestamp }
            val checkCal = lastProc.clone() as Calendar
            var modified = false

            // Make sure we advance checkCal to avoid infinite loops if it gets stuck
            var iterations = 0
            while (iterations < 120) { // Limit to 10 years to prevent bad logic hanging
                iterations++
                
                val candidate = checkCal.clone() as Calendar
                // Set the exact day of month, bounding to maximum of that month (e.g., Feb 28/29 instead of 30)
                val targetDay = minOf(st.dayOfMonth, candidate.getActualMaximum(Calendar.DAY_OF_MONTH))
                candidate.set(Calendar.DAY_OF_MONTH, targetDay)
                candidate.set(Calendar.HOUR_OF_DAY, 0)
                candidate.set(Calendar.MINUTE, 1)
                candidate.set(Calendar.SECOND, 0)
                candidate.set(Calendar.MILLISECOND, 0)

                // If candidate is strictly after lastProcessed and candidate <= today
                if (candidate.timeInMillis > st.lastProcessedTimestamp && candidate.timeInMillis <= today.timeInMillis) {
                    moneyDao.insertTransaction(
                        Transaction(
                            name = st.name,
                            amount = st.amount,
                            description = st.description + " (Scheduled)",
                            timestamp = candidate.timeInMillis,
                            category = st.category,
                            type = st.type
                        )
                    )
                    st.lastProcessedTimestamp = candidate.timeInMillis
                    modified = true
                }

                // Move checkCal forward by 1 month for the next iteration
                checkCal.add(Calendar.MONTH, 1)

                // Stop if checkCal has gone beyond current month and year
                if (checkCal.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
                    (checkCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && checkCal.get(Calendar.MONTH) > today.get(Calendar.MONTH))
                ) {
                    break
                }
            }

            if (modified) {
                moneyDao.updateScheduledTransaction(st)
            }
        }
    }

    /**
     * Calculates and carries over the balance from previous months as "Leftover" INCOME.
     */
    suspend fun processLeftoverCarryovers() {
        val earliestTx = moneyDao.getEarliestTransaction()
        if (earliestTx == null) return

        val today = Calendar.getInstance()
        
        val currentCal = Calendar.getInstance().apply { 
            timeInMillis = earliestTx.timestamp 
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (true) {
            val isBeforeCurrentMonth = currentCal.get(Calendar.YEAR) < today.get(Calendar.YEAR) || 
                                      (currentCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
                                       currentCal.get(Calendar.MONTH) < today.get(Calendar.MONTH))
            
            if (!isBeforeCurrentMonth) break

            val endOfMonth = (currentCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }

            val nextMonth1st = (currentCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            
            val nextMonthEnd = (nextMonth1st.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }

            val existingLeftovers = moneyDao.getLeftoverTransactionsBetween(nextMonth1st.timeInMillis, nextMonthEnd.timeInMillis)
            
            val totalIncome = moneyDao.getTotalIncomeBefore(endOfMonth.timeInMillis) ?: 0.0
            val totalExpense = moneyDao.getTotalExpenseBefore(endOfMonth.timeInMillis) ?: 0.0
            val rawBalance = totalIncome - totalExpense
            val balance = Math.round(rawBalance * 100.0) / 100.0

            if (balance > 0) {
                if (existingLeftovers.isEmpty()) {
                    moneyDao.insertTransaction(
                        Transaction(
                            name = "Leftover",
                            amount = balance,
                            description = "Carryover from previous month",
                            timestamp = nextMonth1st.timeInMillis,
                            category = "Leftover",
                            type = TransactionType.INCOME
                        )
                    )
                    moneyDao.insertCategory(Category("Leftover"))
                } else {
                    val leftover = existingLeftovers.first()
                    if (leftover.amount != balance) {
                        moneyDao.updateTransaction(leftover.copy(amount = balance))
                    }
                    // Delete any duplicate leftovers if they exist
                    for (i in 1 until existingLeftovers.size) {
                        moneyDao.deleteTransaction(existingLeftovers[i])
                    }
                }
            } else {
                // Balance is <= 0, delete any existing leftovers for this month
                existingLeftovers.forEach {
                    moneyDao.deleteTransaction(it)
                }
            }

            currentCal.add(Calendar.MONTH, 1)
        }
    }

    fun getTotalBalance(): Flow<Double> {
        val incomeFlow = moneyDao.getTotalIncome().map { it ?: 0.0 }
        val expenseFlow = moneyDao.getTotalExpense().map { it ?: 0.0 }
        return incomeFlow.combine(expenseFlow) { income, expense ->
            income - expense
        }
    }

    fun getSpendingsBetween(startDate: Long, endDate: Long): Flow<Double> {
        return moneyDao.getSpendingsBetween(startDate, endDate).map { it ?: 0.0 }
    }

    fun getCurrentMonthSpendings(): Flow<Double> {
        val calendar = Calendar.getInstance()
        
        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        // End of month
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis
        
        return moneyDao.getSpendingsBetween(startDate, endDate).map { it ?: 0.0 }
    }

    /**
     * Parses bulk string input in CSV format:
     * Name, dd/MM/yyyy HH:mm, Amount, Type, Category
     * Example: Grocery, 25/12/2023 14:30, 50.0, EXPENSE, Food
     */
    suspend fun parseAndInsertBulk(bulkData: String) {
        val lines = bulkData.split("\n")
        val transactions = mutableListOf<Transaction>()
        val newCategories = mutableSetOf<String>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.split(",")
            if (parts.size >= 5) {
                val name = parts[0].trim()
                val dateStr = parts[1].trim()
                val amountStr = parts[2].trim()
                val typeStr = parts[3].trim()
                val category = parts[4].trim()

                try {
                    val timestamp = dateFormat.parse(dateStr)?.time ?: continue
                    val amount = amountStr.toDouble()
                    val type = TransactionType.valueOf(typeStr.uppercase())
                    
                    transactions.add(
                        Transaction(
                            name = name,
                            amount = amount,
                            description = "",
                            timestamp = timestamp,
                            category = category,
                            type = type
                        )
                    )
                    newCategories.add(category)
                } catch (e: Exception) {
                    // Skip invalid lines
                }
            }
        }
        
        // Insert new categories found in the bulk input
        for (category in newCategories) {
            insertCategory(category)
        }
        
        if (transactions.isNotEmpty()) {
            insertTransactions(transactions)
        }
    }
}
