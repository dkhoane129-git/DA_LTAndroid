package com.spendwise.app.viewmodel

import androidx.annotation.StringRes

/** Trạng thái chung cho các form Login / Register / Forgot Password. */
data class AuthUiState(
    val isLoading: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val confirmError: Int? = null,
    val message: String? = null,
    val isSuccess: Boolean = false
)
