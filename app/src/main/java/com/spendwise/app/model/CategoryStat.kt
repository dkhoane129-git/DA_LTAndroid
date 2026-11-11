package com.spendwise.app.model

data class CategoryStat(
    val category: Category,
    val total: Long,
    val percent: Int
)
