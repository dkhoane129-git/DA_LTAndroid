package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache tỷ giá tải từ API để vẫn xem được khi offline. */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val code: String,
    val base: String,
    val rate: Double,
    val updatedAt: Long
)
