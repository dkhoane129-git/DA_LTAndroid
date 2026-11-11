package com.spendwise.app.network

import android.util.Log
import com.spendwise.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Tạo Retrofit dùng chung 1 OkHttpClient (chung connection pool). */
object ApiClient {

    private val TOKEN_REGEX = Regex("auth=[^&\\s]+")

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor { message ->
                // Che ID token (?auth=...) trước khi ghi log
                Log.d("OkHttp", message.replace(TOKEN_REGEX, "auth=███"))
            }.apply {
                // Bản release không log gì để tránh lộ dữ liệu người dùng
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val exchangeRateApi: ExchangeRateApi by lazy {
        retrofit(BuildConfig.EXCHANGE_BASE_URL).create(ExchangeRateApi::class.java)
    }

    val cloudSyncApi: CloudSyncApi by lazy {
        retrofit(BuildConfig.FIREBASE_DB_URL).create(CloudSyncApi::class.java)
    }
}
