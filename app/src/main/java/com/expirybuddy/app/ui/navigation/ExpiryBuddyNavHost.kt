package com.expirybuddy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expirybuddy.app.ui.screens.details.ItemDetailsScreen
import com.expirybuddy.app.ui.screens.edit.AddEditItemScreen
import com.expirybuddy.app.ui.screens.home.HomeScreen
import com.expirybuddy.app.ui.screens.settings.SettingsScreen

@Composable
fun ExpiryBuddyNavHost(
    openItemId: Long? = null,
    onOpenItemHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val itemIdArg = navArgument(Destinations.ITEM_ID_ARG) {
        type = NavType.LongType
        defaultValue = Destinations.NO_ITEM_ID
    }

    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                onAddItem = { navController.navigate(Destinations.add()) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onOpenItem = { id -> navController.navigate(Destinations.details(id)) },
            )
        }
        // The edit and details ViewModels read the item id from the route's SavedStateHandle.
        composable(Destinations.EDIT, arguments = listOf(itemIdArg)) {
            AddEditItemScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Destinations.DETAILS, arguments = listOf(itemIdArg)) { entry ->
            val itemId = entry.arguments?.getLong(Destinations.ITEM_ID_ARG)
                ?: Destinations.NO_ITEM_ID
            ItemDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.edit(itemId)) },
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }

    // A tapped reminder notification opens that item's details on top of Home.
    LaunchedEffect(openItemId) {
        if (openItemId != null) {
            navController.navigate(Destinations.details(openItemId))
            onOpenItemHandled()
        }
    }
}
