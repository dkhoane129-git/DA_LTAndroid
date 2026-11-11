package com.spendwise.app.model

data class MonthSummary(
    val income: Long = 0,
    val expense: Long = 0
) {
    val balance: Long get() = income - expense
}
