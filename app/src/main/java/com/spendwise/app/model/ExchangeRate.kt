package com.spendwise.app.model

/** 1 [base] = [rate] đơn vị [code]. */
data class ExchangeRate(
    val code: String,
    val name: String,
    val countryCode: String,
    val rate: Double,
    val updatedAt: Long
) {
    /** Link ảnh cờ quốc gia (tải bằng Glide ở Milestone 5). */
    val flagUrl: String get() = "https://flagcdn.com/w80/$countryCode.png"
}
