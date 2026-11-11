package com.spendwise.app.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.spendwise.app.database.entity.PendingDeleteEntity
import com.spendwise.app.database.entity.TransactionEntity
import com.spendwise.app.model.TransactionType
import com.spendwise.app.network.CloudSyncApi
import com.spendwise.app.network.dto.RemoteTransaction
import com.spendwise.app.repository.SyncRepository
import com.spendwise.app.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mô phỏng 2 điện thoại (mỗi máy 1 Room DB riêng) đăng nhập cùng 1 tài khoản,
 * đồng bộ qua 1 "Firebase Realtime Database" giả (MockWebServer).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MultiDeviceSyncTest {

    private val uid = "user-1"
    private lateinit var server: MockWebServer
    private val cloud = linkedMapOf<String, RemoteTransaction>()   // users/{uid}/transactions
    private val keySeq = AtomicInteger()
    private val gson = Gson()

    private lateinit var dbA: AppDatabase
    private lateinit var dbB: AppDatabase
    private lateinit var syncA: SyncRepository
    private lateinit var syncB: SyncRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = FakeRtdb()
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CloudSyncApi::class.java)
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        dbA = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        dbB = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        syncA = SyncRepository(api, dbA.transactionDao(), dbA.pendingDeleteDao(), { "token" }, { uid })
        syncB = SyncRepository(api, dbB.transactionDao(), dbB.pendingDeleteDao(), { "token" }, { uid })
    }

    @After
    fun tearDown() {
        dbA.close(); dbB.close(); server.shutdown()
    }

    private fun tx(title: String, amount: Long, updatedAt: Long = 1_000) = TransactionEntity(
        userId = uid, title = title, amount = amount, type = TransactionType.EXPENSE,
        categoryId = 1, date = 1L, updatedAt = updatedAt
    )

    private suspend fun titles(db: AppDatabase) = db.transactionDao().getAll(uid).map { it.title }.sorted()

    @Test
    fun loginOnNewPhone_pullsEverything() = runBlocking {
        dbA.transactionDao().insertAll(listOf(tx("Cơm", 30_000), tx("Grab", 50_000)))
        assertTrue(syncA.fullSync() is Resource.Success)      // máy A đẩy lên
        assertEquals(2, cloud.size)

        assertEquals(emptyList<String>(), titles(dbB))         // máy B mới đăng nhập, trống
        syncB.fullSync()
        assertEquals(listOf("Cơm", "Grab"), titles(dbB))       // → có đủ dữ liệu
    }

    @Test
    fun addEditDelete_propagateBothWays() = runBlocking {
        dbA.transactionDao().insert(tx("Cơm", 30_000))
        syncA.fullSync(); syncB.fullSync()

        // B thêm mới → A nhận được
        dbB.transactionDao().insert(tx("Trà sữa", 45_000))
        syncB.fullSync(); syncA.fullSync()
        assertEquals(listOf("Cơm", "Trà sữa"), titles(dbA))

        // A sửa (updatedAt mới hơn) → B nhận bản sửa
        val com = dbA.transactionDao().getAll(uid).first { it.title == "Cơm" }
        dbA.transactionDao().update(com.copy(title = "Cơm tấm", updatedAt = 5_000))
        syncA.fullSync(); syncB.fullSync()
        assertEquals(listOf("Cơm tấm", "Trà sữa"), titles(dbB))

        // B xóa (ghi pending delete như TransactionRepository) → A cũng mất
        val tra = dbB.transactionDao().getAll(uid).first { it.title == "Trà sữa" }
        dbB.pendingDeleteDao().insert(PendingDeleteEntity(tra.remoteId!!, uid))
        dbB.transactionDao().deleteById(tra.id)
        syncB.fullSync(); syncA.fullSync()
        assertEquals(listOf("Cơm tấm"), titles(dbA))
        assertEquals(1, cloud.size)
    }

    @Test
    fun conflict_newerUpdateWins() = runBlocking {
        dbA.transactionDao().insert(tx("Xăng", 70_000))
        syncA.fullSync(); syncB.fullSync()
        val a = dbA.transactionDao().getAll(uid).single()
        val b = dbB.transactionDao().getAll(uid).single()
        dbA.transactionDao().update(a.copy(amount = 80_000, updatedAt = 2_000))
        dbB.transactionDao().update(b.copy(amount = 90_000, updatedAt = 3_000))  // B sửa sau
        syncA.fullSync(); syncB.fullSync(); syncA.fullSync()
        assertEquals(90_000L, dbA.transactionDao().getAll(uid).single().amount)
        assertEquals(90_000L, dbB.transactionDao().getAll(uid).single().amount)
    }

    @Test
    fun offline_returnsErrorAndKeepsLocalData() = runBlocking {
        dbA.transactionDao().insert(tx("Cơm", 30_000))
        server.shutdown()
        val r = syncA.fullSync()
        assertTrue(r is Resource.Error && r.code == null)
        assertEquals(listOf("Cơm"), titles(dbA))
    }

    /** Firebase RTDB REST tối giản: GET/POST/PUT/DELETE trên users/{uid}/transactions. */
    private inner class FakeRtdb : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.requestUrl!!.encodedPath
            val listPath = "/users/$uid/transactions.json"
            val key = Regex("^/users/$uid/transactions/([^/]+)\\.json$").find(path)?.groupValues?.get(1)
            return when {
                request.method == "GET" && path == listPath ->
                    MockResponse().setBody(if (cloud.isEmpty()) "null" else gson.toJson(cloud))
                request.method == "POST" && path == listPath -> {
                    val k = "-K${keySeq.incrementAndGet()}"
                    cloud[k] = gson.fromJson(request.body.readUtf8(), RemoteTransaction::class.java)
                    MockResponse().setBody("""{"name":"$k"}""")
                }
                request.method == "PUT" && key != null -> {
                    val body = request.body.readUtf8()
                    cloud[key] = gson.fromJson(body, RemoteTransaction::class.java)
                    MockResponse().setBody(body)
                }
                request.method == "DELETE" && key != null -> {
                    cloud.remove(key); MockResponse().setBody("null")
                }
                else -> MockResponse().setResponseCode(400)
            }
        }
    }

}
