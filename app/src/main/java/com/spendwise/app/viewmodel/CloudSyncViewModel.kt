package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.SyncRepository
import com.spendwise.app.util.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CloudSyncViewModel(
    private val syncRepository: SyncRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _remote = MutableStateFlow<Resource<List<TransactionItem>>>(Resource.Loading)
    /** Danh sách giao dịch trên đám mây (GET) bọc trong Resource. */
    val remote: StateFlow<Resource<List<TransactionItem>>> = _remote.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _remote.value = Resource.Loading
            _remote.value = when (val r = syncRepository.fetchRemote()) {
                is Resource.Success -> {
                    val cats = categoryRepository.categories.first()
                    Resource.Success(Calculations.toItems(r.data, cats))
                }
                is Resource.Error -> Resource.Error(r.message, r.code)
                Resource.Loading -> Resource.Loading
            }
        }
    }

    /** Đồng bộ ngay 2 chiều (bình thường app đã tự đồng bộ). */
    fun syncNow() = runAction {
        when (val r = syncRepository.fullSync()) {
            is Resource.Success -> "Đã đồng bộ: tải về ${r.data.pulled}, tải lên ${r.data.pushed}, xóa ${r.data.deleted}"
            is Resource.Error -> r.message
            Resource.Loading -> null
        }
    }

    fun deleteRemote(key: String) = runAction {
        when (val r = syncRepository.deleteRemote(key)) {
            is Resource.Success -> "Đã xóa bản sao trên đám mây"
            is Resource.Error -> r.message
            Resource.Loading -> null
        }
    }

    private fun runAction(block: suspend () -> String?) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            block()?.let { _messages.send(it) }
            _busy.value = false
            refresh()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = container()
                CloudSyncViewModel(c.syncRepository, c.categoryRepository)
            }
        }
    }
}
