package com.expirybuddy.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand tokens from the design guidelines. Screens never use these directly: they read Material 3
 * roles (see [lightScheme]/[darkScheme]) or the extra roles in [ExpiryBuddyColors].
 */
object Brand {
    val Orange = Color(0xFFF56A2A)
    val OrangeDark = Color(0xFFB94716)
    val Peach = Color(0xFFFFF0E6)
    val Canvas = Color(0xFFFFFCFA)
    val Card = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF7F1ED)
    val TextPrimary = Color(0xFF26211E)
    val TextSecondary = Color(0xFF6B625D)
    val Border = Color(0xFFE6DDD7)
    val StatusExpired = Color(0xFFB3261E)
    val StatusSoon = Color(0xFF9A4D00)
    val StatusUpcoming = Color(0xFF426B9A)
    val StatusSuccess = Color(0xFF386A20)
}

/** Roles Material 3 has no slot for: status colours, the peach wash and the soft border. */
@Immutable
data class ExpiryBuddyColors(
    val peach: Color,
    val border: Color,
    /** Text/icon colour for items that need attention soon. Meets 4.5:1 on the card colour. */
    val soon: Color,
    val expired: Color,
    val upcoming: Color,
    val success: Color,
    val successContainer: Color,
)

val LightExpiryColors = ExpiryBuddyColors(
    peach = Brand.Peach,
    border = Brand.Border,
    // OrangeDark rather than the bright brand orange: bright orange text on white is only ~3:1.
    soon = Brand.OrangeDark,
    expired = Brand.StatusExpired,
    upcoming = Brand.StatusUpcoming,
    success = Brand.StatusSuccess,
    successContainer = Color(0xFFE6F2DF),
)

val DarkExpiryColors = ExpiryBuddyColors(
    peach = Color(0xFF3A2A21),
    border = Color(0xFF4A403A),
    soon = Color(0xFFFFB68A),
    expired = Color(0xFFFFB4AB),
    upcoming = Color(0xFFA9C7F0),
    success = Color(0xFF9ED68B),
    successContainer = Color(0xFF233B1C),
)

val LocalExpiryBuddyColors = staticCompositionLocalOf { LightExpiryColors }
