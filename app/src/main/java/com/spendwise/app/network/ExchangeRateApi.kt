package com.spendwise.app.network

import com.spendwise.app.network.dto.ExchangeRateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {

    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): Response<ExchangeRateResponse>
}
