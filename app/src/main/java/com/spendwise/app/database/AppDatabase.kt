package com.spendwise.app.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spendwise.app.database.dao.BudgetDao
import com.spendwise.app.database.dao.CategoryDao
import com.spendwise.app.database.dao.ExchangeRateDao
import com.spendwise.app.database.dao.PendingDeleteDao
import com.spendwise.app.database.dao.TransactionDao
import com.spendwise.app.database.dao.UserDao
import com.spendwise.app.database.entity.BudgetEntity
import com.spendwise.app.database.entity.CategoryEntity
import com.spendwise.app.database.entity.ExchangeRateEntity
import com.spendwise.app.database.entity.PendingDeleteEntity
import com.spendwise.app.database.entity.TransactionEntity
import com.spendwise.app.database.entity.UserEntity

/**
 * CSDL Room duy nhất của ứng dụng.
 * Singleton Pattern: @Volatile + synchronized đảm bảo cả app chỉ mở 1 kết nối,
 * kể cả khi nhiều luồng gọi getInstance() cùng lúc.
 */
@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ExchangeRateEntity::class,
        PendingDeleteEntity::class
    ],
    version = 3,
    exportSchema = true,
    // v2 (Milestone 4) thêm bảng exchange_rates, Room tự sinh migration nên không mất dữ liệu cũ
    // v3: bảng pending_deletes cho đồng bộ tự động
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun pendingDeleteDao(): PendingDeleteDao

    companion object {
        private const val DB_NAME = "spendwise.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}
