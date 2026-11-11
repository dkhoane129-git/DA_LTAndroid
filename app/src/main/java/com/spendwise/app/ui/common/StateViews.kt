package com.spendwise.app.ui.common

import androidx.core.view.isVisible
import com.spendwise.app.databinding.ViewStateBinding
import com.spendwise.app.util.Resource

/**
 * Hiển thị 3 trạng thái của [Resource] lên layout dùng chung view_state.xml.
 * [hasData] = true khi Error nhưng vẫn còn dữ liệu cache → hiện banner nhỏ thay vì che cả màn.
 */
fun ViewStateBinding.render(resource: Resource<*>, hasData: Boolean, onRetry: () -> Unit) {
    progress.isVisible = resource is Resource.Loading
    val error = resource as? Resource.Error
    errorGroup.isVisible = error != null
    if (error != null) {
        tvError.text = if (hasData) "${error.message}\nĐang hiển thị dữ liệu đã lưu." else error.message
        btnRetry.setOnClickListener { onRetry() }
    }
}
