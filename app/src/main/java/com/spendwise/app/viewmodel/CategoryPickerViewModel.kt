package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.Category
import com.spendwise.app.model.TransactionType
import com.spendwise.app.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CategoryPickerViewModel(
    savedState: SavedStateHandle,
    categoryRepository: CategoryRepository
) : ViewModel() {

    /** null = hiển thị tất cả danh mục (dùng cho bộ lọc ở tab Giao dịch). */
    private val type: TransactionType? =
        savedState.get<String>(ARG_TYPE)?.let { TransactionType.valueOf(it) }

    val categories: StateFlow<List<Category>> =
        (if (type == null) categoryRepository.categories else categoryRepository.categoriesOf(type))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        const val ARG_TYPE = "type"

        val Factory = viewModelFactory {
            initializer { CategoryPickerViewModel(createSavedStateHandle(), container().categoryRepository) }
        }
    }
}
