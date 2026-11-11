package com.spendwise.app.repository

import com.spendwise.app.database.entity.CategoryEntity
import com.spendwise.app.database.entity.TransactionEntity
import com.spendwise.app.model.TransactionType
import java.util.Calendar

/**
 * Dữ liệu khởi tạo: danh mục mặc định (nạp vào Room khi cài app lần đầu)
 * và bộ giao dịch mẫu (chỉ thêm khi người dùng bấm "Tạo dữ liệu mẫu").
 */
object MockData {

    val defaultCategories = listOf(
        CategoryEntity(1, "Ăn uống", "🍜", TransactionType.EXPENSE),
        CategoryEntity(2, "Di chuyển", "🛵", TransactionType.EXPENSE),
        CategoryEntity(3, "Mua sắm", "🛍️", TransactionType.EXPENSE),
        CategoryEntity(4, "Hóa đơn", "🧾", TransactionType.EXPENSE),
        CategoryEntity(5, "Giải trí", "🎮", TransactionType.EXPENSE),
        CategoryEntity(6, "Học tập", "📚", TransactionType.EXPENSE),
        CategoryEntity(7, "Lương", "💼", TransactionType.INCOME),
        CategoryEntity(8, "Thưởng", "🎁", TransactionType.INCOME),
        CategoryEntity(9, "Làm thêm", "💻", TransactionType.INCOME)
    )

    fun sampleTransactions(userId: String): List<TransactionEntity> {
        fun daysAgo(days: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        fun tx(title: String, amount: Long, type: TransactionType, cat: Long, day: Int, note: String = "") =
            TransactionEntity(
                userId = userId, title = title, amount = amount, type = type,
                categoryId = cat, date = daysAgo(day), note = note
            )

        return listOf(
            tx("Lương tháng", 8_000_000, TransactionType.INCOME, 7, 1),
            tx("Cơm trưa", 35_000, TransactionType.EXPENSE, 1, 0),
            tx("Đổ xăng", 70_000, TransactionType.EXPENSE, 2, 1),
            tx("Tiền điện", 450_000, TransactionType.EXPENSE, 4, 2, "Hóa đơn EVN"),
            tx("Trà sữa", 45_000, TransactionType.EXPENSE, 1, 3),
            tx("Mua áo", 320_000, TransactionType.EXPENSE, 3, 4),
            tx("Làm freelance", 1_500_000, TransactionType.INCOME, 9, 5),
            tx("Xem phim", 120_000, TransactionType.EXPENSE, 5, 6),
            tx("Sách Kotlin", 180_000, TransactionType.EXPENSE, 6, 8),
            tx("Grab", 55_000, TransactionType.EXPENSE, 2, 10),
            tx("Tiền mạng", 220_000, TransactionType.EXPENSE, 4, 12),
            tx("Ăn tối", 150_000, TransactionType.EXPENSE, 1, 15),
            tx("Thưởng dự án", 1_000_000, TransactionType.INCOME, 8, 35),
            tx("Lương tháng", 8_000_000, TransactionType.INCOME, 7, 31),
            tx("Siêu thị", 640_000, TransactionType.EXPENSE, 3, 33),
            tx("Học phí khóa học", 1_200_000, TransactionType.EXPENSE, 6, 40)
        )
    }
}
