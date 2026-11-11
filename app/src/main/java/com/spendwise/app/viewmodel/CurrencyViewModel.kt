package com.spendwise.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.ExchangeRate
import com.spendwise.app.repository.CurrencyRepository
import com.spendwise.app.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val savedState: SavedStateHandle,
    private val repository: CurrencyRepository
) : ViewModel() {

    data class RateRow(val rate: ExchangeRate, val converted: Double)

    private val _state = MutableStateFlow<Resource<List<ExchangeRate>>>(Resource.Loading)
    /** Trạng thái tải tỷ giá: Loading / Success / Error. */
    val state: StateFlow<Resource<List<ExchangeRate>>> = _state.asStateFlow()

    private val amount: StateFlow<Long> = savedState.getStateFlow(KEY_AMOUNT, DEFAULT_AMOUNT)

    /** Danh sách đổi tiền: lấy từ Success, hoặc dữ liệu cache khi Error (offline). */
    val rows: StateFlow<List<RateRow>> = combine(_state, amount) { state, vnd ->
        val rates = when (state) {
            is Resource.Success -> state.data
            is Resource.Error -> state.data.orEmpty()
            Resource.Loading -> emptyList()
        }
        rates.map { RateRow(it, vnd * it.rate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getRates().collect { _state.value = it }
        }
    }

    fun setAmount(text: String) {
        savedState[KEY_AMOUNT] = text.toLongOrNull() ?: 0L
    }

    companion object {
        private const val KEY_AMOUNT = "amount"
        const val DEFAULT_AMOUNT = 1_000_000L

        val Factory = viewModelFactory {
            initializer { CurrencyViewModel(createSavedStateHandle(), container().currencyRepository) }
        }
    }
}
