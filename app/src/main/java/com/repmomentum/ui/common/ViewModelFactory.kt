package com.repmomentum.ui.common

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.repmomentum.AppContainer
import com.repmomentum.RepMomentumApplication

/** Fabrique de ViewModel branchée sur l'[AppContainer] de l'application. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as RepMomentumApplication
        create(app.container, createSavedStateHandle())
    }
}
