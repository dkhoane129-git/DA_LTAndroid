package com.spendwise.app.model

/** Một giao dịch thu / chi. [amount] luôn dương, chiều tiền do [type] quyết định. */
data class Transaction(
    val id: Long,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    val date: Long,
    val note: String = "",
    val remoteId: String? = null
)
