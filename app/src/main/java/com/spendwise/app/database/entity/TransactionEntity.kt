package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionType

/** Phân hệ Giao dịch. Mỗi giao dịch gắn với [userId] để nhiều tài khoản dùng chung 1 máy. */
@Entity(
    tableName = "transactions",
    indices = [Index("userId"), Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    val date: Long,
    val note: String = "",
    /** Khóa của bản ghi trên server (dùng khi đồng bộ đám mây). null = chưa đồng bộ. */
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun TransactionEntity.toModel() = Transaction(id, title, amount, type, categoryId, date, note, remoteId)

fun Transaction.toEntity(userId: String) = TransactionEntity(
    id = id.coerceAtLeast(0),
    userId = userId,
    title = title,
    amount = amount,
    type = type,
    categoryId = categoryId,
    date = date,
    note = note,
    remoteId = remoteId
)
