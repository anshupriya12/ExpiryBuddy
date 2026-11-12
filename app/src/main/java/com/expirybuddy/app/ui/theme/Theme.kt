package com.expirybuddy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Brand.Orange,
    onPrimary = Color.White,
    primaryContainer = Brand.Peach,
    onPrimaryContainer = Brand.OrangeDark,
    secondary = Brand.OrangeDark,
    onSecondary = Color.White,
    secondaryContainer = Brand.Subtle,
    onSecondaryContainer = Brand.TextPrimary,
    tertiary = Brand.StatusUpcoming,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3ECF7),
    onTertiaryContainer = Color(0xFF1D3350),
    error = Brand.StatusExpired,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF410E0B),
    background = Brand.Canvas,
    onBackground = Brand.TextPrimary,
    surface = Brand.Canvas,
    onSurface = Brand.TextPrimary,
    surfaceVariant = Brand.Subtle,
    onSurfaceVariant = Brand.TextSecondary,
    outline = Brand.Border,
    outlineVariant = Brand.Border,
    surfaceContainerLowest = Brand.Card,
    surfaceContainerLow = Brand.Card,
    surfaceContainer = Brand.Subtle,
    surfaceContainerHigh = Brand.Subtle,
    surfaceContainerHighest = Color(0xFFF0E8E2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A57),
    onPrimary = Color(0xFF3A1500),
    primaryContainer = Color(0xFF4A2A18),
    onPrimaryContainer = Color(0xFFFFD9C6),
    secondary = Color(0xFFFFB68A),
    onSecondary = Color(0xFF3A1500),
    secondaryContainer = Color(0xFF332B26),
    onSecondaryContainer = Color(0xFFF1E9E4),
    tertiary = Color(0xFFA9C7F0),
    onTertiary = Color(0xFF0C2542),
    tertiaryContainer = Color(0xFF28415F),
    onTertiaryContainer = Color(0xFFD7E6FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1511),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1816),
    onBackground = Color(0xFFF1E9E4),
    surface = Color(0xFF1C1816),
    onSurface = Color(0xFFF1E9E4),
    surfaceVariant = Color(0xFF332B26),
    onSurfaceVariant = Color(0xFFBDB1AA),
    outline = Color(0xFF4A403A),
    outlineVariant = Color(0xFF4A403A),
    surfaceContainerLowest = Color(0xFF26201D),
    surfaceContainerLow = Color(0xFF26201D),
    surfaceContainer = Color(0xFF2B2420),
    surfaceContainerHigh = Color(0xFF332B26),
    surfaceContainerHighest = Color(0xFF3B322D),
)

/**
 * The Expiry Buddy theme. Dynamic (wallpaper) colour is deliberately not used: the warm orange palette
 * is the brand, so it must look the same on every phone. Dark mode follows the system setting.
 */
@Composable
fun ExpiryBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExpiryBuddyColors provides if (darkTheme) DarkExpiryColors else LightExpiryColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            // Text and icons outside a Surface (a transparent Scaffold, a bare Column) would otherwise
            // default to black, which disappears on the dark background.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                content = content,
            )
        }
    }
}

/** Extra Expiry Buddy colour roles, next to `MaterialTheme.colorScheme`. */
val MaterialTheme.expiryColors: ExpiryBuddyColors
    @Composable get() = LocalExpiryBuddyColors.current
