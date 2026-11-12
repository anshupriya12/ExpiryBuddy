package com.expirybuddy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One radius per component role: fields and chips are small, cards medium, sheets large. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Pill shape for primary buttons, the search field and the add button. */
val PillShape = RoundedCornerShape(50)

/** The spacing scale. Nothing in the UI uses a gap outside it. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Horizontal page margin shared by every screen. */
    val page: Dp = 20.dp

    /** Minimum size of any tappable control. */
    val touchTarget: Dp = 48.dp
}
