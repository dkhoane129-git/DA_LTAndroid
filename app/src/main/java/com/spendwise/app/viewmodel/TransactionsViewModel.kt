package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.Category
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.model.TransactionType
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Bộ lọc được lưu trong SavedStateHandle nên giữ nguyên khi xoay màn hình
 * và cả khi hệ thống kill process.
 */
class TransactionsViewModel(
    private val savedState: SavedStateHandle,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel(), TransactionListSource {

    enum class TypeFilter(val type: TransactionType?) {
        ALL(null), INCOME(TransactionType.INCOME), EXPENSE(TransactionType.EXPENSE)
    }

    val typeFilter: StateFlow<TypeFilter> = savedState.getStateFlow(KEY_TYPE, TypeFilter.ALL)
    val categoryFilter: StateFlow<Long> = savedState.getStateFlow(KEY_CATEGORY, NO_CATEGORY)
    private val query: StateFlow<String> = savedState.getStateFlow(KEY_QUERY, "")

    val selectedCategory: StateFlow<Category?> =
        combine(categoryFilter, categoryRepository.categories) { id, cats -> cats.find { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    override val transactionItems: StateFlow<List<TransactionItem>> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        typeFilter,
        categoryFilter,
        query
    ) { tx, cats, type, categoryId, q ->
        val filtered = Calculations.filter(
            tx, type.type, categoryId.takeIf { it != NO_CATEGORY }, q
        )
        Calculations.toItems(filtered, cats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(filter: TypeFilter) {
        savedState[KEY_TYPE] = filter
    }

    fun setCategoryFilter(categoryId: Long) {
        savedState[KEY_CATEGORY] = categoryId
    }

    fun clearCategoryFilter() = setCategoryFilter(NO_CATEGORY)

    fun setQuery(text: String) {
        savedState[KEY_QUERY] = text
    }

    companion object {
        const val NO_CATEGORY = -1L
        private const val KEY_TYPE = "type_filter"
        private const val KEY_CATEGORY = "category_filter"
        private const val KEY_QUERY = "query"

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                TransactionsViewModel(createSavedStateHandle(), c.transactionRepository, c.categoryRepository)
            }
        }
    }
}
