package com.spendwise.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import com.spendwise.app.database.AppDatabase
import com.spendwise.app.network.ApiClient
import com.spendwise.app.repository.AuthRepository
import com.spendwise.app.repository.BudgetRepository
import com.spendwise.app.repository.CategoryRepository
import com.spendwise.app.repository.CurrencyRepository
import com.spendwise.app.repository.SessionManager
import com.spendwise.app.repository.SyncManager
import com.spendwise.app.repository.SyncRepository
import com.spendwise.app.repository.TransactionRepository

/** Dependency container thủ công: tạo 1 lần trong Application, ViewModel lấy qua Factory. */
class AppContainer(context: Context, applicationScope: CoroutineScope) {

    private val database = AppDatabase.getInstance(context)

    val sessionManager = SessionManager(context)
    val authRepository = AuthRepository(FirebaseAuth.getInstance(), database.userDao(), sessionManager)
    val categoryRepository = CategoryRepository(database.categoryDao())
    val transactionRepository =
        TransactionRepository(database.transactionDao(), database.pendingDeleteDao(), sessionManager)
    val budgetRepository = BudgetRepository(database.budgetDao(), sessionManager)

    // Milestone 4: API online
    val currencyRepository = CurrencyRepository(ApiClient.exchangeRateApi, database.exchangeRateDao())

    /** Milestone 5: ID token lấy từ Firebase Auth (AuthRepository implement TokenProvider). */
    val syncRepository = SyncRepository(
        ApiClient.cloudSyncApi, database.transactionDao(), database.pendingDeleteDao(),
        tokenProvider = authRepository,
        currentUid = { sessionManager.uid.value },
        onSynced = { sessionManager.lastSyncAt = it }
    )

    /** Đồng bộ tự động giữa các thiết bị cùng tài khoản. */
    val syncManager = SyncManager(context, syncRepository, sessionManager, applicationScope).also { sync ->
        transactionRepository.onLocalChange = sync::requestSync
    }
}
