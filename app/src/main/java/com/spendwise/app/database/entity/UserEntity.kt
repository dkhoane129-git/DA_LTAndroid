package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spendwise.app.model.User

/** Phân hệ Auth / Tài khoản: hồ sơ người dùng lưu cục bộ để dùng offline. */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val avatarUrl: String? = null
)

fun UserEntity.toModel() = User(uid, fullName, email, phone, avatarUrl)
fun User.toEntity() = UserEntity(uid, fullName, email, phone, avatarUrl)
