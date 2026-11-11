package com.spendwise.app.model

data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val avatarUrl: String? = null
)
