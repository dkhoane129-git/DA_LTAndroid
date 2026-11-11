package com.spendwise.app.network.dto

import com.google.gson.annotations.SerializedName

/** Một giao dịch lưu trên Firebase Realtime Database (REST JSON). */
data class RemoteTransaction(
    @SerializedName("title") val title: String = "",
    @SerializedName("amount") val amount: Long = 0,
    @SerializedName("type") val type: String = "EXPENSE",
    @SerializedName("categoryId") val categoryId: Long = 0,
    @SerializedName("date") val date: Long = 0,
    @SerializedName("note") val note: String = "",
    @SerializedName("updatedAt") val updatedAt: Long = 0
)

/** Phản hồi của POST: {"name": "-Nxyz..."} là khóa vừa được tạo. */
data class PushResponse(@SerializedName("name") val name: String)
