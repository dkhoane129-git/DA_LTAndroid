package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.MonthSummary
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.repository.AuthRepository
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.SyncManager
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class HomeViewModel(
    private val syncManager: SyncManager,
    authRepository: AuthRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel(), TransactionListSource {

    /** Trạng thái đồng bộ tự động (hiển thị dưới lời chào). */
    val syncState: StateFlow<SyncManager.State> = syncManager.state

    fun syncNow() = syncManager.requestSync()

    val userName: StateFlow<String> = authRepository.currentUser
        .map { it?.fullName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Tổng thu / chi / số dư của tháng hiện tại. */
    val summary: StateFlow<MonthSummary> = transactionRepository.transactions
        .map { list ->
            val now = Calendar.getInstance()
            Calculations.summary(Calculations.inMonth(list, now.get(Calendar.YEAR), now.get(Calendar.MONTH)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthSummary())

    /** 5 giao dịch gần nhất. */
    override val transactionItems: StateFlow<List<TransactionItem>> =
        combine(transactionRepository.transactions, categoryRepository.categories) { tx, cats ->
            Calculations.toItems(tx, cats).take(RECENT_LIMIT)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        private const val RECENT_LIMIT = 5

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                HomeViewModel(c.syncManager, c.authRepository, c.transactionRepository, c.categoryRepository)
            }
        }
    }
}
