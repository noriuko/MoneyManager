package com.example.moneymanager.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @androidx.room.Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND NOT (name = 'Leftover' AND category = 'Leftover')")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC LIMIT 1")
    suspend fun getEarliestTransaction(): Transaction?

    @Query("SELECT * FROM transactions WHERE name = 'Leftover' AND category = 'Leftover' AND timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun getLeftoverTransactionsBetween(startDate: Long, endDate: Long): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND NOT (name = 'Leftover' AND category = 'Leftover') AND timestamp <= :timestamp")
    suspend fun getTotalIncomeBefore(timestamp: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp <= :timestamp")
    suspend fun getTotalExpenseBefore(timestamp: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getSpendingsBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT name FROM categories ORDER BY name ASC")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateTransactionsCategory(oldName: String, newName: String)

    @Query("UPDATE scheduled_transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateScheduledTransactionsCategory(oldName: String, newName: String)

    // Scheduled Transactions
    @Query("SELECT * FROM scheduled_transactions ORDER BY dayOfMonth ASC")
    fun getAllScheduledTransactions(): Flow<List<ScheduledTransaction>>

    @Query("SELECT * FROM scheduled_transactions")
    suspend fun getAllScheduledTransactionsSync(): List<ScheduledTransaction>

    @Query("SELECT * FROM scheduled_transactions WHERE id = :id")
    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTransaction(scheduledTransaction: ScheduledTransaction)

    @androidx.room.Update
    suspend fun updateScheduledTransaction(scheduledTransaction: ScheduledTransaction)

    @Delete
    suspend fun deleteScheduledTransaction(scheduledTransaction: ScheduledTransaction)
}
