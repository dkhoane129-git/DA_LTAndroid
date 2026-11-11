package com.spendwise.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendwise.app.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates WHERE base = :base")
    fun observeRates(base: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE base = :base")
    suspend fun getRates(base: String): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRateEntity>)
}
