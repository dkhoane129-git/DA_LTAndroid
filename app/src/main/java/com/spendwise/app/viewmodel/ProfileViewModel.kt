package com.spendwise.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.model.User
import com.spendwise.app.repository.AuthRepository
import com.spendwise.app.repository.TransactionRepository
import kotlinx.coroutines.launch

/** Ví dụ dùng LiveData (các màn khác dùng StateFlow) — cả hai đều đáp ứng yêu cầu. */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val user: LiveData<User?> = authRepository.currentUser.asLiveData()

    val transactionCount: LiveData<Int> = transactionRepository.count.asLiveData()

    fun addSampleData() {
        viewModelScope.launch { transactionRepository.addSampleData() }
    }

    fun logout() = authRepository.logout()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = container()
                ProfileViewModel(c.authRepository, c.transactionRepository)
            }
        }
    }
}
