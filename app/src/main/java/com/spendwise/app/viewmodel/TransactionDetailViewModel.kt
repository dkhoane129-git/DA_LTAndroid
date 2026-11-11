package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionDetailViewModel(
    savedState: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val item: TransactionItem) : State
        /** Giao dịch đã bị xóa / không tồn tại → Activity tự đóng. */
        data object NotFound : State
    }

    val transactionId: Long = savedState[EXTRA_TRANSACTION_ID] ?: -1L

    val state: StateFlow<State> = combine(
        transactionRepository.observe(transactionId),
        categoryRepository.categories
    ) { t, cats ->
        if (t == null) State.NotFound
        else State.Loaded(TransactionItem(t, cats.find { it.id == t.categoryId }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State.Loading)

    fun delete() {
        viewModelScope.launch { transactionRepository.delete(transactionId) }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                TransactionDetailViewModel(createSavedStateHandle(), c.transactionRepository, c.categoryRepository)
            }
        }
    }
}
