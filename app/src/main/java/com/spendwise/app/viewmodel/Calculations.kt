package com.spendwise.app.viewmodel

import com.spendwise.app.model.Category
import com.spendwise.app.model.CategoryStat
import com.spendwise.app.model.MonthSummary
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.model.TransactionType
import com.spendwise.app.util.Formatters
import kotlin.math.roundToInt

/** Các hàm tính toán thuần (không phụ thuộc Android) → dễ viết unit test. */
object Calculations {

    fun toItems(transactions: List<Transaction>, categories: List<Category>): List<TransactionItem> {
        val byId = categories.associateBy { it.id }
        return transactions.sortedByDescending { it.date }.map { TransactionItem(it, byId[it.categoryId]) }
    }

    fun filter(
        transactions: List<Transaction>,
        type: TransactionType?,
        categoryId: Long?,
        query: String
    ): List<Transaction> = transactions
        .filter { type == null || it.type == type }
        .filter { categoryId == null || it.categoryId == categoryId }
        .filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }

    fun inMonth(transactions: List<Transaction>, year: Int, month: Int) =
        transactions.filter { Formatters.isInMonth(it.date, year, month) }

    fun summary(transactions: List<Transaction>) = MonthSummary(
        income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    )

    /** Tránh tràn Long khi tính tỷ lệ chi tiêu / hạn mức. Giới hạn ở Int.MAX_VALUE. */
    fun budgetPercent(expense: Long, limit: Long?): Int {
        if (expense <= 0L || limit == null || limit <= 0L) return 0
        return ((expense.toDouble() / limit.toDouble()) * 100.0)
            .coerceAtMost(Int.MAX_VALUE.toDouble())
            .toInt()
    }

    fun categoryStats(
        transactions: List<Transaction>,
        categories: List<Category>,
        type: TransactionType
    ): List<CategoryStat> {
        val ofType = transactions.filter { it.type == type }
        val total = ofType.sumOf { it.amount }
        val byId = categories.associateBy { it.id }
        return ofType.groupBy { it.categoryId }
            .map { (catId, list) ->
                // Giữ giao dịch của danh mục đã xóa, không làm mất tiền trong báo cáo.
                val cat = byId[catId] ?: Category(catId, "Danh mục đã xóa", "📁", type)
                val sum = list.sumOf { it.amount }
                val percent = if (total <= 0L) 0 else (sum.toDouble() / total * 100).roundToInt()
                CategoryStat(cat, sum, percent.coerceIn(0, 100))
            }
            .sortedByDescending { it.total }
    }
}
