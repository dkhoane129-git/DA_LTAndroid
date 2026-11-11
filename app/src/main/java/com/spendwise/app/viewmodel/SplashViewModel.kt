package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendwise.app.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

    enum class Destination { AUTH, MAIN }

    private val _destination = MutableStateFlow<Destination?>(null)
    val destination: StateFlow<Destination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1_000)
            _destination.value =
                if (authRepository.isLoggedIn()) Destination.MAIN else Destination.AUTH
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SplashViewModel(container().authRepository) }
        }
    }
}
