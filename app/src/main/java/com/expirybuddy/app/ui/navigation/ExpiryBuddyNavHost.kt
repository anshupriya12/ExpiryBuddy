package com.expirybuddy.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expirybuddy.app.R
import com.expirybuddy.app.ui.screens.all.AllItemsScreen
import com.expirybuddy.app.ui.screens.details.ItemDetailsScreen
import com.expirybuddy.app.ui.screens.edit.AddEditItemScreen
import com.expirybuddy.app.ui.screens.home.HomeScreen
import com.expirybuddy.app.ui.screens.onboarding.OnboardingScreen
import com.expirybuddy.app.ui.screens.settings.SettingsScreen

/**
 * The app's navigation graph.
 *
 * @param startDestination [Destinations.ONBOARDING] on first launch, otherwise [Destinations.HOME].
 * @param onOnboardingFinished records that onboarding is done, so it is not shown again.
 */
@Composable
fun ExpiryBuddyNavHost(
    startDestination: String,
    onOnboardingFinished: () -> Unit,
    openItemId: Long? = null,
    onOpenItemHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val itemIdArg = navArgument(Destinations.ITEM_ID_ARG) {
        type = NavType.LongType
        defaultValue = Destinations.NO_ITEM_ID
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    onOnboardingFinished()
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.HOME) { entry ->
            val message by entry.messageState()
            HomeScreen(
                onAddItem = { navController.navigate(Destinations.add()) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onOpenItem = { id -> navController.navigate(Destinations.details(id)) },
                onSeeAll = { navController.navigate(Destinations.ALL_ITEMS) },
                snackbarHostState = remember { SnackbarHostState() },
                message = message,
                onMessageShown = entry::clearMessage,
            )
        }
        composable(Destinations.ALL_ITEMS) { entry ->
            val message by entry.messageState()
            AllItemsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Destinations.details(id)) },
                snackbarHostState = remember { SnackbarHostState() },
                message = message,
                onMessageShown = entry::clearMessage,
            )
        }
        // The edit and details ViewModels read the item id from the route's SavedStateHandle.
        composable(Destinations.EDIT, arguments = listOf(itemIdArg)) {
            AddEditItemScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { wasEditing ->
                    navController.leaveWithMessage(
                        if (wasEditing) R.string.message_changes_saved else R.string.message_item_saved,
                    )
                },
                // The item is gone, so its details screen (just below this one) must go too.
                onDeleted = { navController.leaveDeletedItem() },
            )
        }
        composable(Destinations.DETAILS, arguments = listOf(itemIdArg)) { entry ->
            val itemId = entry.arguments?.getLong(Destinations.ITEM_ID_ARG)
                ?: Destinations.NO_ITEM_ID
            val message by entry.messageState()
            ItemDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.edit(itemId)) },
                onDeleted = { navController.leaveWithMessage(R.string.message_item_deleted) },
                onMarkedUsed = { navController.leaveWithMessage(R.string.message_marked_used) },
                snackbarHostState = remember { SnackbarHostState() },
                message = message,
                onMessageShown = entry::clearMessage,
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBackToHome = {
                    // Settings is only reachable from Home, so this is the same as Back.
                    if (!navController.popBackStack(Destinations.HOME, inclusive = false)) {
                        navController.popBackStack()
                    }
                },
            )
        }
    }

    // A tapped reminder notification opens that item's details on top of the current screen.
    LaunchedEffect(openItemId) {
        if (openItemId != null) {
            navController.navigate(Destinations.details(openItemId))
            onOpenItemHandled()
        }
    }
}

/** The one-time message left for this screen by the screen above it (a string resource id). */
@Composable
private fun NavBackStackEntry.messageState() =
    savedStateHandle.getStateFlow<Int?>(Destinations.MESSAGE_KEY, null).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearMessage() {
    savedStateHandle[Destinations.MESSAGE_KEY] = null
}

/** Goes back one screen and leaves [message] to be shown there as a snackbar. */
private fun NavController.leaveWithMessage(@StringRes message: Int) {
    previousBackStackEntry?.savedStateHandle?.set(Destinations.MESSAGE_KEY, message)
    popBackStack()
}

/** After deleting from the edit screen: drops Edit and Details together and confirms on the list screen. */
private fun NavController.leaveDeletedItem() {
    if (popBackStack(Destinations.DETAILS, inclusive = true)) {
        currentBackStackEntry?.savedStateHandle?.set(Destinations.MESSAGE_KEY, R.string.message_item_deleted)
    } else {
        leaveWithMessage(R.string.message_item_deleted)
    }
}
