package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Giao dịch đã xóa trên máy nhưng chưa xóa được trên cloud (vd: đang offline).
 * Lần đồng bộ sau sẽ gửi @DELETE rồi xóa dòng này, tránh việc giao dịch "sống lại".
 */
@Entity(tableName = "pending_deletes")
data class PendingDeleteEntity(
    @PrimaryKey val remoteId: String,
    val userId: String
)
