package com.spendwise.app.repository

import com.spendwise.app.database.dao.PendingDeleteDao
import com.spendwise.app.database.dao.TransactionDao
import com.spendwise.app.database.entity.TransactionEntity
import com.spendwise.app.database.entity.toModel
import com.spendwise.app.model.Transaction
import com.spendwise.app.model.TransactionType
import com.spendwise.app.network.CloudSyncApi
import com.spendwise.app.network.TokenProvider
import com.spendwise.app.network.dto.RemoteTransaction
import com.spendwise.app.network.safeApiCall
import com.spendwise.app.util.Resource

/** Đồng bộ giao dịch giữa Room (local) và Firebase Realtime Database (REST). */
class SyncRepository(
    private val api: CloudSyncApi,
    private val dao: TransactionDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val tokenProvider: TokenProvider,
    /** uid người dùng hiện tại (null = chưa đăng nhập). */
    private val currentUid: () -> String?,
    /** Gọi khi đồng bộ xong để lưu thời điểm. */
    private val onSynced: (Long) -> Unit = {}
) {

    /** Kết quả 1 lần đồng bộ 2 chiều. */
    data class SyncResult(val pulled: Int, val pushed: Int, val deleted: Int)

    /**
     * Đồng bộ 2 chiều (gọi tự động):
     * 1. Gửi các lệnh xóa đang chờ (@DELETE).
     * 2. Kéo dữ liệu cloud về (@GET): thêm bản mới, cập nhật bản cloud mới hơn,
     *    xóa local những bản đã bị xóa ở máy khác.
     * 3. Đẩy dữ liệu local lên (@POST bản chưa có, @PUT bản local mới hơn).
     * Xung đột: bản có updatedAt lớn hơn thắng.
     */
    suspend fun fullSync(): Resource<SyncResult> {
        val uid = currentUid() ?: return notLoggedIn()
        val token = tokenProvider.idToken() ?: return Resource.Error("Chưa xác thực", 401)

        // 1. Xóa trên cloud những giao dịch đã xóa ở máy này
        var deleted = 0
        for (pending in pendingDeleteDao.getAll(uid)) {
            when (val r = safeApiCall { api.delete(uid, pending.remoteId, token) }) {
                is Resource.Success -> { pendingDeleteDao.remove(pending.remoteId); deleted++ }
                is Resource.Error -> if (r.code == 404) pendingDeleteDao.remove(pending.remoteId)
                    else return Resource.Error(r.message, r.code)
                Resource.Loading -> Unit
            }
        }

        // 2. Kéo về
        val remote = when (val r = safeApiCall { api.getAll(uid, token) }) {
            is Resource.Success -> r.data.orEmpty()
            is Resource.Error -> return Resource.Error(r.message, r.code)
            Resource.Loading -> return Resource.Loading
        }
        val local = dao.getAll(uid)
        val localByRemoteId = local.filter { it.remoteId != null }.associateBy { it.remoteId!! }
        var pulled = 0

        for ((key, r) in remote) {
            val l = localByRemoteId[key]
            when {
                l == null -> { dao.insert(r.toEntity(uid, key)); pulled++ }
                r.updatedAt > l.updatedAt -> { dao.update(r.toEntity(uid, key).copy(id = l.id)); pulled++ }
            }
        }
        // Có remoteId nhưng không còn trên cloud → đã bị xóa ở máy khác
        for (l in local) {
            if (l.remoteId != null && l.remoteId !in remote) { dao.delete(l); deleted++ }
        }

        // 3. Đẩy lên
        var pushed = 0
        for (l in local) {
            val remoteId = l.remoteId
            if (remoteId == null) {
                when (val r = safeApiCall { api.create(uid, token, l.toRemote()) }) {
                    is Resource.Success -> { r.data?.name?.let { dao.setRemoteId(l.id, it) }; pushed++ }
                    is Resource.Error -> return Resource.Error(r.message, r.code)
                    Resource.Loading -> Unit
                }
            } else {
                val r = remote[remoteId] ?: continue
                if (l.updatedAt > r.updatedAt) {
                    val res = safeApiCall { api.update(uid, remoteId, token, l.toRemote()) }
                    if (res is Resource.Error) return Resource.Error(res.message, res.code)
                    pushed++
                }
            }
        }

        onSynced(System.currentTimeMillis())
        return Resource.Success(SyncResult(pulled, pushed, deleted))
    }

    /** GET: danh sách giao dịch đang có trên đám mây. */
    suspend fun fetchRemote(): Resource<List<Transaction>> {
        val uid = currentUid() ?: return notLoggedIn()
        return when (val r = safeApiCall { api.getAll(uid, tokenProvider.idToken()) }) {
            is Resource.Success -> Resource.Success(
                r.data.orEmpty().map { (key, remote) -> remote.toModel(key) }.sortedByDescending { it.date }
            )
            is Resource.Error -> Resource.Error(r.message, r.code)
            Resource.Loading -> Resource.Loading
        }
    }

    /** DELETE một bản ghi trên mây (bản local giữ nguyên, chỉ bỏ liên kết). */
    suspend fun deleteRemote(key: String): Resource<Unit> {
        val uid = currentUid() ?: return notLoggedIn()
        return when (val r = safeApiCall { api.delete(uid, key, tokenProvider.idToken()) }) {
            is Resource.Success -> {
                dao.getByRemoteId(key)?.let { dao.update(it.copy(remoteId = null)) }
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(r.message, r.code)
            Resource.Loading -> Resource.Loading
        }
    }

    private fun <T> notLoggedIn(): Resource<T> = Resource.Error("Bạn chưa đăng nhập", 401)

    private fun TransactionEntity.toRemote() =
        RemoteTransaction(title, amount, type.name, categoryId, date, note, updatedAt)

    private fun RemoteTransaction.toEntity(uid: String, key: String) = TransactionEntity(
        userId = uid, title = title, amount = amount,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        categoryId = categoryId, date = date, note = note, remoteId = key, updatedAt = updatedAt
    )

    private fun RemoteTransaction.toModel(key: String) = toEntity("", key).toModel()
}
