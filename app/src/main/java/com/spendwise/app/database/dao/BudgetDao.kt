package com.spendwise.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.spendwise.app.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE userId = :userId AND year = :year AND month = :month LIMIT 1")
    fun observeBudget(userId: String, year: Int, month: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun observeAll(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND year = :year AND month = :month LIMIT 1")
    suspend fun getBudget(userId: String, year: Int, month: Int): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}
