package com.spendwise.app.repository

import com.spendwise.app.database.dao.CategoryDao
import com.spendwise.app.database.entity.CategoryEntity
import com.spendwise.app.database.entity.toModel
import com.spendwise.app.model.Category
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {

    val categories: Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun categoriesOf(type: TransactionType): Flow<List<Category>> =
        dao.observeByType(type).map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): Category? = dao.getById(id)?.toModel()

    /** Nạp danh mục mặc định khi CSDL còn trống (lần đầu cài app). */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() == 0) dao.insertAll(MockData.defaultCategories)
    }

    suspend fun add(name: String, icon: String, type: TransactionType) {
        dao.insert(CategoryEntity(name = name, icon = icon, type = type))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
