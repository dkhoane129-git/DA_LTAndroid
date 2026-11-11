package com.spendwise.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendwise.app.database.entity.PendingDeleteEntity

@Dao
interface PendingDeleteDao {

    @Query("SELECT * FROM pending_deletes WHERE userId = :userId")
    suspend fun getAll(userId: String): List<PendingDeleteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingDeleteEntity)

    @Query("DELETE FROM pending_deletes WHERE remoteId = :remoteId")
    suspend fun remove(remoteId: String)
}
