package com.spendwise.app.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendwise.app.database.entity.BudgetEntity
import com.spendwise.app.database.entity.CategoryEntity
import com.spendwise.app.database.entity.TransactionEntity
import com.spendwise.app.database.entity.UserEntity
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Kiểm thử Room trên JVM (Robolectric) — không cần máy ảo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RoomDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun singleton_returnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertSame(AppDatabase.getInstance(context), AppDatabase.getInstance(context))
    }

    @Test
    fun transactionDao_flowEmitsAfterInsertUpdateDelete() = runBlocking {
        val dao = db.transactionDao()
        assertEquals(0, dao.observeAll("u1").first().size)

        val id = dao.insert(
            TransactionEntity(userId = "u1", title = "Cơm", amount = 30_000,
                type = TransactionType.EXPENSE, categoryId = 1, date = 1L)
        )
        dao.insert(
            TransactionEntity(userId = "u2", title = "Của người khác", amount = 1,
                type = TransactionType.EXPENSE, categoryId = 1, date = 1L)
        )
        assertEquals(listOf("Cơm"), dao.observeAll("u1").first().map { it.title })

        dao.update(dao.getById(id)!!.copy(title = "Cơm tấm"))
        assertEquals("Cơm tấm", dao.observeById(id).first()?.title)

        dao.deleteById(id)
        assertNull(dao.observeById(id).first())
        assertEquals(0, dao.observeAll("u1").first().size)
    }

    @Test
    fun categoryDao_filterByType_andConverter() = runBlocking {
        val dao = db.categoryDao()
        dao.insertAll(
            listOf(
                CategoryEntity(1, "Ăn uống", "🍜", TransactionType.EXPENSE),
                CategoryEntity(7, "Lương", "💼", TransactionType.INCOME)
            )
        )
        assertEquals(listOf("Lương"), dao.observeByType(TransactionType.INCOME).first().map { it.name })
        assertEquals(2, dao.count())
    }

    @Test
    fun userDao_upsertAndUpdateProfile() = runBlocking {
        val dao = db.userDao()
        dao.upsert(UserEntity("u1", "Khoa", "k@vlu.edu.vn"))
        dao.updateProfile("u1", "Đăng Khoa", "0909", null)
        val user = dao.observeUser("u1").first()!!
        assertEquals("Đăng Khoa", user.fullName)
        assertEquals("0909", user.phone)
    }

    @Test
    fun budgetDao_uniquePerMonth() = runBlocking {
        val dao = db.budgetDao()
        dao.upsert(BudgetEntity(userId = "u1", year = 2026, month = 10, limitAmount = 5_000_000))
        val existing = dao.getBudget("u1", 2026, 10)!!
        dao.upsert(existing.copy(limitAmount = 6_000_000))
        assertEquals(1, dao.observeAll("u1").first().size)
        assertEquals(6_000_000L, dao.observeBudget("u1", 2026, 10).first()?.limitAmount)
    }
}
