package com.spendwise.app.viewmodel

import com.spendwise.app.model.TransactionItem
import kotlinx.coroutines.flow.StateFlow

/**
 * TransactionListFragment (Fragment dùng chung) lấy dữ liệu qua interface này,
 * nên dùng lại được ở cả HomeFragment và TransactionsFragment.
 */
interface TransactionListSource {
    val transactionItems: StateFlow<List<TransactionItem>>
}
