package com.spendwise.app.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.spendwise.app.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tự động đồng bộ khi:
 * - đăng nhập (kể cả trên máy mới) và lúc mở app,
 * - app quay lại foreground,
 * - thêm / sửa / xóa giao dịch,
 * - thiết bị có mạng trở lại.
 * Các yêu cầu dồn dập được gộp lại (debounce) và chỉ 1 lần đồng bộ chạy tại một thời điểm.
 */
@OptIn(FlowPreview::class)
class SyncManager(
    context: Context,
    private val syncRepository: SyncRepository,
    private val session: SessionManager,
    private val scope: CoroutineScope
) {

    sealed interface State {
        data object Idle : State
        data object Syncing : State
        data class Done(val at: Long) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(
        session.lastSyncAt.takeIf { it > 0 }?.let { State.Done(it) } ?: State.Idle
    )
    val state: StateFlow<State> = _state.asStateFlow()

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val mutex = Mutex()

    init {
        scope.launch {
            requests.debounce(DEBOUNCE_MS).collect { runSync() }
        }
        // Đăng nhập / mở app với phiên cũ → kéo dữ liệu về ngay
        scope.launch {
            session.uid.filterNotNull().distinctUntilChanged().collect { requestSync() }
        }
        registerNetworkCallback(context)
    }

    fun requestSync() {
        requests.tryEmit(Unit)
    }

    private suspend fun runSync() {
        if (session.uid.value == null) return
        mutex.withLock {
            _state.value = State.Syncing
            _state.value = when (val r = syncRepository.fullSync()) {
                is Resource.Success -> State.Done(System.currentTimeMillis())
                is Resource.Error -> State.Failed(r.message)
                Resource.Loading -> State.Idle
            }
        }
    }

    private fun registerNetworkCallback(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = requestSync()
        })
    }

    private companion object {
        const val DEBOUNCE_MS = 1_500L
    }
}
