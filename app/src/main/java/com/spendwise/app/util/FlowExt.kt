package com.spendwise.app.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Thu thập Flow an toàn theo vòng đời: chỉ collect khi UI ở trạng thái STARTED,
 * tự dừng khi vào nền và tự chạy lại khi quay về (kể cả sau khi xoay màn hình).
 */
fun <T> Flow<T>.collectWithLifecycle(owner: LifecycleOwner, action: suspend (T) -> Unit) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { action(it) }
        }
    }
}
