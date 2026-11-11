package com.spendwise.app.model

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val type: TransactionType
)
