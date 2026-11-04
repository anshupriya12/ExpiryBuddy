package com.expirybuddy.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expirybuddy.app.AppContainer
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.ui.screens.details.ItemDetailsViewModel
import com.expirybuddy.app.ui.screens.edit.AddEditViewModel
import com.expirybuddy.app.ui.screens.home.HomeViewModel

/** Builds ViewModels from the app-level [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(createSavedStateHandle(), container().itemRepository, container().clock)
        }
        initializer {
            AddEditViewModel(createSavedStateHandle(), container().itemRepository, container().clock)
        }
        initializer { ItemDetailsViewModel(createSavedStateHandle(), container().itemRepository) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as ExpiryBuddyApp).container
