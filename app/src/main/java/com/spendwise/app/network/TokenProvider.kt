package com.spendwise.app.network

/** Cung cấp ID token để gọi API cần xác thực. null = chưa có (server sẽ trả 401). */
fun interface TokenProvider {
    suspend fun idToken(): String?
}
