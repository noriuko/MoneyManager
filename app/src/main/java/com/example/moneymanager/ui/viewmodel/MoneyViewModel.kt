package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.core.data.Transaction
import com.example.moneymanager.core.data.ScheduledTransaction
import com.example.moneymanager.core.repository.MoneyRepository
import com.example.moneymanager.core.data.TransactionType
import com.example.moneymanager.core.util.FormatUtils
import com.example.moneymanager.ui.viewmodel.DailyTransactionGroup
import com.example.moneymanager.ui.viewmodel.TransactionUiModel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

import com.example.moneymanager.core.repository.UserPreferencesRepository
import com.example.moneymanager.core.repository.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        userPreferencesRepository.setThemeMode(mode)
    }

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

    val groupedTransactionsForSelectedMonth = transactionsForSelectedMonth.map { transactions ->
        if (transactions.isEmpty()) return@map emptyList()

        val cal = Calendar.getInstance()
        val locale = Locale.getDefault()

        transactions
            .sortedByDescending { it.timestamp }
            .groupBy { transaction ->
                cal.timeInMillis = transaction.timestamp
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .map { (dayStartMillis, dayTxList) ->
                var dailyInc = 0.0
                var dailyExp = 0.0

                val uiModels = dayTxList.map { tx ->
                    val isInc = tx.type == TransactionType.INCOME
                    if (isInc) dailyInc += tx.amount else dailyExp += tx.amount

                    val timeStr = FormatUtils.formatTime(tx.timestamp)
                    val subtitle = if (tx.description.isNotBlank()) {
                        "$timeStr • ${tx.description}"
                    } else {
                        timeStr
                    }

                    TransactionUiModel(
                        transaction = tx,
                        id = tx.id,
                        name = tx.name,
                        subtitle = subtitle,
                        formattedAmount = String.format(locale, "%s€%.2f", if (isInc) "+" else "-", tx.amount),
                        category = tx.category,
                        isIncome = isInc
                    )
                }

                DailyTransactionGroup(
                    dateMillis = dayStartMillis,
                    dayOfMonth = FormatUtils.formatDay(dayStartMillis),
                    dayOfWeek = FormatUtils.formatDayOfWeek(dayStartMillis),
                    monthYear = FormatUtils.formatMonthYear(dayStartMillis),
                    formattedDailyIncome = if (dailyInc > 0) String.format(locale, "+€%.2f", dailyInc) else null,
                    formattedDailyExpense = if (dailyExp > 0) String.format(locale, "-€%.2f", dailyExp) else null,
                    transactions = uiModels
                )
            }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncomeForSelectedMonth = transactionsForSelectedMonth.map { transactions ->
        transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else 0.0 }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
