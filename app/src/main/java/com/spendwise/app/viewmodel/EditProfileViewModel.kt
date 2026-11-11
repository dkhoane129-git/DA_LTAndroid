package com.spendwise.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.R
import com.spendwise.app.model.User
import com.spendwise.app.repository.AuthRepository
import kotlinx.coroutines.launch

class EditProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val user: LiveData<User?> = authRepository.currentUser.asLiveData()

    /** Chỉ đổ dữ liệu vào form 1 lần (lần nạp đầu tiên), tránh ghi đè chữ người dùng đang gõ. */
    var formFilled = false

    private val _nameError = MutableLiveData<Int?>(null)
    val nameError: LiveData<Int?> = _nameError

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    fun save(name: String, phone: String) {
        if (name.isBlank()) {
            _nameError.value = R.string.error_name
            return
        }
        _nameError.value = null
        viewModelScope.launch {
            authRepository.updateProfile(name.trim(), phone.trim())
            _saved.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EditProfileViewModel(container().authRepository) }
        }
    }
}
