package com.spendwise.app.model

/** Model hiển thị: giao dịch kèm danh mục để Adapter không phải tự tra cứu. */
data class TransactionItem(
    val transaction: Transaction,
    val category: Category?
)
