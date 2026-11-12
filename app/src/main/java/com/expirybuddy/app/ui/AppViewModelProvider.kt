package com.expirybuddy.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expirybuddy.app.AppContainer
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.ui.screens.all.AllItemsViewModel
import com.expirybuddy.app.ui.screens.details.ItemDetailsViewModel
import com.expirybuddy.app.ui.screens.edit.AddEditViewModel
import com.expirybuddy.app.ui.screens.home.HomeViewModel
import com.expirybuddy.app.ui.screens.settings.SettingsViewModel

/** Builds ViewModels from the app-level [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(createSavedStateHandle(), container().itemRepository, container().clock)
        }
        initializer {
            AllItemsViewModel(createSavedStateHandle(), container().itemRepository, container().clock)
        }
        initializer {
            AddEditViewModel(
                createSavedStateHandle(),
                container().itemRepository,
                container().clock,
                container().photoStorage,
                container().userPreferences,
            )
        }
        initializer {
            ItemDetailsViewModel(
                createSavedStateHandle(),
                container().itemRepository,
                container().clock,
                container().photoStorage,
            )
        }
        initializer { SettingsViewModel(container().userPreferences) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as ExpiryBuddyApp).container
