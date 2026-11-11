package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.R
import com.spendwise.app.model.Category
import com.spendwise.app.model.TransactionType
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel(
    private val savedState: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val selectedType: StateFlow<TransactionType> =
        savedState.getStateFlow(KEY_TYPE, TransactionType.EXPENSE)

    val categories: StateFlow<List<Category>> = selectedType
        .flatMapLatest { categoryRepository.categoriesOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<Int>(Channel.BUFFERED)
    /** Thông báo một lần (string resource id) cho Snackbar. */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun selectType(type: TransactionType) {
        savedState[KEY_TYPE] = type
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _messages.send(R.string.error_name)
                return@launch
            }
            categoryRepository.add(name.trim(), icon.ifBlank { "📁" }, selectedType.value)
            _messages.send(R.string.msg_category_added)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            if (transactionRepository.isCategoryInUse(id)) {
                _messages.send(R.string.msg_category_in_use)
                return@launch
            }
            categoryRepository.delete(id)
            _messages.send(R.string.msg_category_deleted)
        }
    }

    companion object {
        private const val KEY_TYPE = "type"

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                CategoryViewModel(createSavedStateHandle(), c.categoryRepository, c.transactionRepository)
            }
        }
    }
}
