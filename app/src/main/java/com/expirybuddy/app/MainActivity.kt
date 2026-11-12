package com.expirybuddy.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.expirybuddy.app.ui.navigation.Destinations
import com.expirybuddy.app.ui.navigation.ExpiryBuddyNavHost
import com.expirybuddy.app.ui.theme.ExpiryBuddyTheme

class MainActivity : ComponentActivity() {

    /** Item to open because the user tapped its reminder notification. Cleared once handled. */
    private var openItemId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after rotation or process restore the same intent is replayed.
        if (savedInstanceState == null) openItemId = itemIdFrom(intent)

        val preferences = (application as ExpiryBuddyApp).container.userPreferences
        // Decided once per launch. The nav graph pops onboarding itself when it finishes.
        val startDestination = if (preferences.onboardingCompleted) Destinations.HOME else Destinations.ONBOARDING

        setContent {
            ExpiryBuddyTheme {
                ExpiryBuddyNavHost(
                    startDestination = startDestination,
                    onOnboardingFinished = { preferences.onboardingCompleted = true },
                    openItemId = openItemId,
                    onOpenItemHandled = { openItemId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        itemIdFrom(intent)?.let { openItemId = it }
    }

    private fun itemIdFrom(intent: Intent?): Long? =
        intent?.takeIf { it.hasExtra(EXTRA_ITEM_ID) }?.getLongExtra(EXTRA_ITEM_ID, -1L)?.takeIf { it > 0 }

    companion object {
        const val EXTRA_ITEM_ID = "com.expirybuddy.app.extra.ITEM_ID"
    }
}
