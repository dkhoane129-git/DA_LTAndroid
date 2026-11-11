package com.spendwise.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpendWiseApp : Application() {

    lateinit var container: AppContainer
        private set

    /** Scope sống cùng ứng dụng, dùng cho các tác vụ khởi tạo. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)
        applicationScope.launch {
            container.categoryRepository.seedDefaultsIfEmpty()
        }
        // Mỗi lần app quay lại foreground → đồng bộ để thấy dữ liệu mới từ máy khác
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = container.syncManager.requestSync()
        })
    }
}
