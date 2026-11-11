package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.spendwise.app.AppContainer
import com.spendwise.app.SpendWiseApp

/** Lấy AppContainer trong initializer của viewModelFactory. */
internal fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as SpendWiseApp).container
