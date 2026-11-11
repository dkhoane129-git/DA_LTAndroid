package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Phân hệ Thống kê: hạn mức chi tiêu theo tháng của từng người dùng. */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId", "year", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Long
)
