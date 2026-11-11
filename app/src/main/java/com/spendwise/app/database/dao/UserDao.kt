package com.spendwise.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.spendwise.app.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY fullName")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("UPDATE users SET fullName = :fullName, phone = :phone, avatarUrl = :avatarUrl WHERE uid = :uid")
    suspend fun updateProfile(uid: String, fullName: String, phone: String, avatarUrl: String?)

    @Delete
    suspend fun delete(user: UserEntity)
}
