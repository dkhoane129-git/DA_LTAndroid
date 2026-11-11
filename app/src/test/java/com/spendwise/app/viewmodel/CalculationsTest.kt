package com.spendwise.app.viewmodel

import com.spendwise.app.model.Category
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionType.EXPENSE
import com.spendwise.app.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalculationsTest {

    private val now = System.currentTimeMillis()
    private val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis

    private val categories = listOf(
        Category(1, "Ăn uống", "🍜", EXPENSE),
        Category(2, "Di chuyển", "🛵", EXPENSE),
        Category(7, "Lương", "💼", INCOME)
    )
    private val tx = listOf(
        Transaction(1, "Cơm trưa", 40_000, EXPENSE, 1, now),
        Transaction(2, "Grab", 60_000, EXPENSE, 2, now),
        Transaction(3, "Lương tháng", 8_000_000, INCOME, 7, now),
        Transaction(4, "Ăn tối", 100_000, EXPENSE, 1, now),
        Transaction(5, "Cơm năm ngoái", 999_000, EXPENSE, 1, lastYear)
    )

    @Test
    fun filter_byType() {
        val result = Calculations.filter(tx, INCOME, null, "")
        assertEquals(listOf(3L), result.map { it.id })
    }

    @Test
    fun filter_byCategoryAndQuery_caseInsensitive() {
        val result = Calculations.filter(tx, null, 1, "CƠM")
        assertEquals(setOf(1L, 5L), result.map { it.id }.toSet())
    }

    @Test
    fun summary_onlyCurrentMonth() {
        val cal = Calendar.getInstance()
        val s = Calculations.summary(Calculations.inMonth(tx, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
        assertEquals(8_000_000, s.income)
        assertEquals(200_000, s.expense)
        assertEquals(7_800_000, s.balance)
    }

    @Test
    fun categoryStats_sortedAndPercent() {
        val cal = Calendar.getInstance()
        val month = Calculations.inMonth(tx, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val stats = Calculations.categoryStats(month, categories, EXPENSE)
        assertEquals(listOf(1L, 2L), stats.map { it.category.id })
        assertEquals(140_000, stats[0].total)
        assertEquals(70, stats[0].percent)
        assertEquals(30, stats[1].percent)
    }

    @Test
    fun toItems_newestFirst_withCategory() {
        val items = Calculations.toItems(tx, categories)
        assertEquals(5L, items.last().transaction.id)
        assertTrue(items.all { it.category != null })
    }
}
