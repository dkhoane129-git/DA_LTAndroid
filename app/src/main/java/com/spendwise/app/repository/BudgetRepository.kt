package com.spendwise.app.repository

import com.spendwise.app.database.dao.BudgetDao
import com.spendwise.app.database.entity.BudgetEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepository(
    private val dao: BudgetDao,
    private val session: SessionManager
) {

    /** Hạn mức chi tiêu của tháng (null = chưa đặt). */
    fun observeLimit(year: Int, month: Int): Flow<Long?> = session.uid.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else dao.observeBudget(uid, year, month).map { it?.limitAmount }
    }

    suspend fun setLimit(year: Int, month: Int, amount: Long) {
        val uid = session.uid.value ?: return
        val existing = dao.getBudget(uid, year, month)
        dao.upsert(
            existing?.copy(limitAmount = amount)
                ?: BudgetEntity(userId = uid, year = year, month = month, limitAmount = amount)
        )
    }
}
