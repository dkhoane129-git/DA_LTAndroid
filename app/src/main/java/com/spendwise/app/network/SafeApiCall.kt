package com.spendwise.app.network

import com.google.gson.JsonParseException
import com.spendwise.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Gọi API trên Dispatchers.IO và chuyển mọi kết quả về [Resource]:
 * mã HTTP lỗi → Resource.Error kèm code; mất mạng / timeout → Resource.Error không code.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T?> =
    withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) Resource.Success(response.body())
            else Resource.Error(httpMessage(response.code()), response.code())
        } catch (e: SocketTimeoutException) {
            Resource.Error("Hết thời gian chờ máy chủ phản hồi")
        } catch (e: IOException) {
            Resource.Error("Không có kết nối mạng. Kiểm tra Wi-Fi / dữ liệu di động")
        } catch (e: JsonParseException) {
            Resource.Error("Dữ liệu trả về không đúng định dạng")
        }
    }

fun httpMessage(code: Int): String = when (code) {
    400 -> "Yêu cầu không hợp lệ (HTTP 400)"
    401 -> "Chưa xác thực hoặc phiên đăng nhập hết hạn (HTTP 401)"
    403 -> "Không có quyền truy cập (HTTP 403)"
    404 -> "Không tìm thấy dữ liệu (HTTP 404)"
    429 -> "Gửi quá nhiều yêu cầu, thử lại sau (HTTP 429)"
    in 500..599 -> "Máy chủ đang gặp sự cố (HTTP $code)"
    else -> "Lỗi kết nối (HTTP $code)"
}
