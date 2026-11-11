package com.spendwise.app.network.dto

import com.google.gson.annotations.SerializedName

/** JSON từ https://open.er-api.com/v6/latest/{base} */
data class ExchangeRateResponse(
    @SerializedName("result") val result: String?,
    @SerializedName("base_code") val baseCode: String?,
    @SerializedName("time_last_update_unix") val updatedAt: Long?,
    @SerializedName("rates") val rates: Map<String, Double>?
)
