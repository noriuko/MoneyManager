package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.core.data.ScheduledTransaction
import com.example.moneymanager.core.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.processLeftoverCarryovers()
            repository.processScheduledTransactions()
        }
    }

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar: StateFlow<Calendar> = _currentCalendar.asStateFlow()

    fun previousMonth() {
        _currentCalendar.value = (_currentCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
    }

    fun nextMonth() {
        _currentCalendar.value = (_currentCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
    }

    // A pair of (startDate, endDate) for the currently selected month
    private val currentMonthRange = _currentCalendar.map { calendar ->
        val startCal = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }
        Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    val transactionsForSelectedMonth = currentMonthRange.flatMapLatest { (start, end) ->
        repository.getTransactionsBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendingsForSelectedMonth = currentMonthRange.flatMapLatest { (start, end) ->
        repository.getSpendingsBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    val categoryBreakdownForSelectedMonth = transactionsForSelectedMonth.map { txList ->
        txList.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance = repository.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categories = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.id == 0L) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
            // Implicitly add category if not exists
            repository.insertCategory(transaction.category)
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            repository.insertCategory(categoryName)
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameCategory(oldName, newName)
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryName)
        }
    }

    fun addBulkTransactions(bulkData: String) {
        viewModelScope.launch {
            repository.parseAndInsertBulk(bulkData)
        }
    }

    // --- Scheduled Transactions ---

    val scheduledTransactions = repository.getAllScheduledTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction? {
        return repository.getScheduledTransactionById(id)
    }

    fun addScheduledTransaction(st: ScheduledTransaction) {
        viewModelScope.launch {
            if (st.id == 0L) {
                repository.insertScheduledTransaction(st)
            } else {
                repository.updateScheduledTransaction(st)
            }
            repository.insertCategory(st.category)
        }
    }

    fun deleteScheduledTransaction(st: ScheduledTransaction) {
        viewModelScope.launch {
            repository.deleteScheduledTransaction(st)
        }
    }
}
