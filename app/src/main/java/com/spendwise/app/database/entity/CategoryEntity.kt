package com.spendwise.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spendwise.app.model.Category
import com.spendwise.app.model.TransactionType

/** Phân hệ Danh mục. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val type: TransactionType
)

fun CategoryEntity.toModel() = Category(id, name, icon, type)
fun Category.toEntity() = CategoryEntity(id, name, icon, type)
