package com.spendwise.app.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.R
import com.spendwise.app.model.Category
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionType
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Toàn bộ dữ liệu form nằm trong ViewModel, nên khi xoay màn hình
 * loại giao dịch, danh mục, ngày đã chọn... không bị mất.
 */
class AddEditTransactionViewModel(
    savedState: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    data class FormState(
        val title: String = "",
        val amountText: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val categoryId: Long? = null,
        val date: Long = System.currentTimeMillis(),
        val note: String = "",
        val remoteId: String? = null,
        val isSaving: Boolean = false,
        @StringRes val titleError: Int? = null,
        @StringRes val amountError: Int? = null,
        @StringRes val categoryError: Int? = null
    )

    sealed interface Event {
        /** Dữ liệu cũ đã nạp xong từ Room (chế độ Sửa) → đổ vào các ô nhập. */
        data class Loaded(val form: FormState) : Event
        data object Saved : Event
    }

    private val editingId: Long = savedState[EXTRA_TRANSACTION_ID] ?: NEW_ID
    val isEditMode: Boolean get() = editingId != NEW_ID

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    val selectedCategory: StateFlow<Category?> =
        combine(_form, categoryRepository.categories) { f, cats -> cats.find { it.id == f.categoryId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        if (isEditMode) {
            viewModelScope.launch {
                val t = transactionRepository.getById(editingId) ?: return@launch
                val loaded = FormState(
                    title = t.title,
                    amountText = t.amount.toString(),
                    type = t.type,
                    categoryId = t.categoryId,
                    date = t.date,
                    note = t.note,
                    remoteId = t.remoteId
                )
                _form.value = loaded
                _events.send(Event.Loaded(loaded))
            }
        }
    }

    fun onTitleChanged(value: String) = _form.update { it.copy(title = value, titleError = null) }
    fun onAmountChanged(value: String) = _form.update { it.copy(amountText = value, amountError = null) }
    fun onNoteChanged(value: String) = _form.update { it.copy(note = value) }
    fun onDateSelected(millis: Long) = _form.update { it.copy(date = millis) }
    fun onCategorySelected(id: Long) = _form.update { it.copy(categoryId = id, categoryError = null) }

    fun onTypeChanged(type: TransactionType) = _form.update {
        // Đổi loại thì danh mục cũ (thu/chi) không còn hợp lệ
        if (it.type == type) it else it.copy(type = type, categoryId = null)
    }

    fun save() {
        val f = _form.value
        if (f.isSaving) return
        val amount = f.amountText.toLongOrNull()
        val titleError = if (f.title.isBlank()) R.string.error_title else null
        val amountError = if (amount == null || amount <= 0) R.string.error_amount else null
        val categoryError = if (f.categoryId == null) R.string.error_category else null
        _form.update { it.copy(titleError = titleError, amountError = amountError, categoryError = categoryError) }
        if (titleError != null || amountError != null || categoryError != null) return

        val transaction = Transaction(
            id = editingId,
            title = f.title.trim(),
            amount = amount!!,
            type = f.type,
            categoryId = f.categoryId!!,
            date = f.date,
            note = f.note.trim(),
            remoteId = f.remoteId
        )
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            if (isEditMode) transactionRepository.update(transaction)
            else transactionRepository.add(transaction)
            _events.send(Event.Saved)
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val NEW_ID = -1L

        val Factory = viewModelFactory {
            initializer {
                val c = container()
                AddEditTransactionViewModel(createSavedStateHandle(), c.transactionRepository, c.categoryRepository)
            }
        }
    }
}
