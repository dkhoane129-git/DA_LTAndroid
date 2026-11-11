package com.spendwise.app.util

/**
 * Bọc kết quả gọi mạng thành 3 trạng thái UI:
 * - [Loading]  → hiện ProgressBar
 * - [Success]  → nạp dữ liệu lên RecyclerView
 * - [Error]    → báo lỗi (mất mạng, HTTP 400/401/500...). [data] là dữ liệu cache (nếu có) để vẫn hiển thị.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(
        val message: String,
        val code: Int? = null,
        val data: T? = null
    ) : Resource<T>()
}
