package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.R
import com.spendwise.app.repository.AuthRepository
import com.spendwise.app.util.Validator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val emailError = if (!Validator.isValidEmail(email)) R.string.error_email else null
        val passwordError = if (!Validator.isValidPassword(password)) R.string.error_password else null
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.login(email.trim(), password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, message = e.message) } }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { LoginViewModel(container().authRepository) }
        }
    }
}
