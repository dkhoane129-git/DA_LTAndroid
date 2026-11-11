package com.spendwise.app.util

import android.util.Patterns

object Validator {
    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean = password.length >= 6
}
