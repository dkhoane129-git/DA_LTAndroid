package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.CategoryStat
import com.spendwise.app.model.MonthSummary
import com.spendwise.app.model.TransactionType
import com.spendwise.app.repository.BudgetRepository
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val savedState: SavedStateHandle,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    data class UiState(
        val year: Int,
        val month: Int,
        val type: TransactionType = TransactionType.EXPENSE,
        val summary: MonthSummary = MonthSummary(),
        val stats: List<CategoryStat> = emptyList(),
        /** Hạn mức chi tiêu tháng, null = chưa đặt. */
        val budget: Long? = null
    ) {
        val budgetPercent: Int
            get() = if (budget == null || budget == 0L) 0
            else ((summary.expense * 100) / budget).toInt()
    }

    private val now = Calendar.getInstance()
    private val year = savedState.getStateFlow(KEY_YEAR, now.get(Calendar.YEAR))
    private val month = savedState.getStateFlow(KEY_MONTH, now.get(Calendar.MONTH))
    private val type = savedState.getStateFlow(KEY_TYPE, TransactionType.EXPENSE)

    private val budget = combine(year, month) { y, m -> y to m }
        .flatMapLatest { (y, m) -> budgetRepository.observeLimit(y, m) }

    val uiState: StateFlow<UiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        combine(year, month, type) { y, m, t -> Triple(y, m, t) },
        budget
    ) { tx, cats, (y, m, t), limit ->
        val inMonth = Calculations.inMonth(tx, y, m)
        UiState(
            year = y, month = m, type = t,
            summary = Calculations.summary(inMonth),
            stats = Calculations.categoryStats(inMonth, cats, t),
            budget = limit
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UiState(year.value, month.value)
    )

    fun selectMonth(year: Int, month: Int) {
        savedState[KEY_YEAR] = year
        savedState[KEY_MONTH] = month
    }

    fun selectType(type: TransactionType) {
        savedState[KEY_TYPE] = type
    }

    fun setBudget(amountText: String): Boolean {
        val amount = amountText.toLongOrNull()?.takeIf { it > 0 } ?: return false
        viewModelScope.launch { budgetRepository.setLimit(year.value, month.value, amount) }
        return true
    }

    companion object {
        private const val KEY_YEAR = "year"
        private const val KEY_MONTH = "month"
        private const val KEY_TYPE = "type"

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                StatisticsViewModel(
                    createSavedStateHandle(), c.transactionRepository,
                    c.categoryRepository, c.budgetRepository
                )
            }
        }
    }
}
