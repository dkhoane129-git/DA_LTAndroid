package com.spendwise.app.repository

import com.spendwise.app.database.dao.PendingDeleteDao
import com.spendwise.app.database.dao.TransactionDao
import com.spendwise.app.database.entity.PendingDeleteEntity
import com.spendwise.app.database.entity.toEntity
import com.spendwise.app.database.entity.toModel
import com.spendwise.app.model.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Room là nguồn dữ liệu duy nhất (single source of truth) cho giao dịch. */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRepository(
    private val dao: TransactionDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val session: SessionManager
) {

    /** Gọi sau mỗi lần dữ liệu thay đổi → SyncManager tự đẩy lên cloud. */
    var onLocalChange: () -> Unit = {}

    /** Giao dịch của người dùng hiện tại, tự đổi khi đăng nhập tài khoản khác. */
    val transactions: Flow<List<Transaction>> = session.uid.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else dao.observeAll(uid).map { list -> list.map { it.toModel() } }
    }

    val count: Flow<Int> = session.uid.flatMapLatest { uid ->
        if (uid == null) flowOf(0) else dao.observeCount(uid)
    }

    fun observe(id: Long): Flow<Transaction?> = dao.observeById(id).map { it?.toModel() }

    suspend fun getById(id: Long): Transaction? = dao.getById(id)?.toModel()

    suspend fun isCategoryInUse(categoryId: Long): Boolean = dao.countByCategory(categoryId) > 0

    suspend fun add(transaction: Transaction): Long {
        val uid = requireUid()
        return dao.insert(transaction.toEntity(uid).copy(id = 0)).also { onLocalChange() }
    }

    suspend fun update(transaction: Transaction) {
        val uid = requireUid()
        dao.update(transaction.toEntity(uid).copy(updatedAt = System.currentTimeMillis()))
        onLocalChange()
    }

    suspend fun delete(id: Long) {
        val entity = dao.getById(id) ?: return
        // Đã có trên cloud → ghi nhớ để xóa trên cloud ở lần đồng bộ tới (kể cả khi đang offline)
        entity.remoteId?.let { pendingDeleteDao.insert(PendingDeleteEntity(it, entity.userId)) }
        dao.deleteById(id)
        onLocalChange()
    }

    suspend fun addSampleData() {
        dao.insertAll(MockData.sampleTransactions(requireUid()))
        onLocalChange()
    }

    private fun requireUid(): String =
        session.uid.value ?: throw IllegalStateException("Chưa đăng nhập")
}
