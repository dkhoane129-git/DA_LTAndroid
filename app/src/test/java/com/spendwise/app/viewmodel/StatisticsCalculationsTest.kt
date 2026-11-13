package com.spendwise.app.viewmodel

import com.spendwise.app.model.Category
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionType.EXPENSE
import com.spendwise.app.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Các ca biên của phần SV B – Thống kê. */
class StatisticsCalculationsTest {

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }.timeInMillis

    @Test fun emptyMonth_hasZeroSummaryAndStats() {
        val month = Calculations.inMonth(emptyList(), 2026, Calendar.OCTOBER)
        assertEquals(0L, Calculations.summary(month).income)
        assertEquals(0L, Calculations.summary(month).expense)
        assertTrue(Calculations.categoryStats(month, emptyList(), EXPENSE).isEmpty())
    }

    @Test fun monthFilter_handlesYearBoundary() {
        val tx = listOf(
            Transaction(1, "Tháng 12", 10, EXPENSE, 1, date(2025, Calendar.DECEMBER, 31)),
            Transaction(2, "Tháng 1", 20, EXPENSE, 1, date(2026, Calendar.JANUARY, 1)),
            Transaction(3, "Tháng 2", 30, EXPENSE, 1, date(2026, Calendar.FEBRUARY, 1))
        )
        assertEquals(listOf(2L), Calculations.inMonth(tx, 2026, Calendar.JANUARY).map { it.id })
    }

    @Test fun categoryStats_excludesOtherTransactionType() {
        val cats = listOf(Category(1, "Ăn", "🍜", EXPENSE), Category(2, "Lương", "💼", INCOME))
        val tx = listOf(
            Transaction(1, "Ăn", 40, EXPENSE, 1, 1L),
            Transaction(2, "Lương", 60, INCOME, 2, 1L)
        )
        val expenseStats = Calculations.categoryStats(tx, cats, EXPENSE)
        val incomeStats = Calculations.categoryStats(tx, cats, INCOME)
        assertEquals(1, expenseStats.size)
        assertEquals(40L, expenseStats[0].total)
        assertEquals(100, expenseStats[0].percent)
        assertEquals(60L, incomeStats[0].total)
    }

    @Test fun deletedCategory_stillCountsInReport() {
        val tx = listOf(Transaction(1, "Cũ", 50_000, EXPENSE, 999, 1L))
        val stats = Calculations.categoryStats(tx, emptyList(), EXPENSE)
        assertEquals(1, stats.size)
        assertEquals(999L, stats[0].category.id)
        assertEquals(50_000L, stats[0].total)
        assertEquals(100, stats[0].percent)
    }

    @Test fun budgetPercent_handlesUnsetExactExceededAndHugeAmounts() {
        assertEquals(0, Calculations.budgetPercent(100, null))
        assertEquals(0, Calculations.budgetPercent(100, 0))
        assertEquals(0, Calculations.budgetPercent(0, 1_000))
        assertEquals(50, Calculations.budgetPercent(500, 1_000))
        assertEquals(100, Calculations.budgetPercent(1_000, 1_000))
        assertEquals(150, Calculations.budgetPercent(1_500, 1_000))
        assertEquals(Int.MAX_VALUE, Calculations.budgetPercent(Long.MAX_VALUE, 1))
    }
}
