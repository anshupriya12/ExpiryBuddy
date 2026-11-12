package com.expirybuddy.app.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.expirybuddy.app.ui.theme.expiryColors

/** Palette the illustrations draw with: thin dark outlines, a restrained orange, pastel washes. */
private class Ink(
    val line: Color,
    val orange: Color,
    val peach: Color,
    val paper: Color,
)

private const val WIDTH = 300f
private const val HEIGHT = 240f

/**
 * Simple 2D line illustrations for the three onboarding pages, drawn in code so they scale with the screen,
 * follow the theme and add nothing to the download. Decorative only, so they have no description.
 */
@Composable
fun OnboardingIllustration(page: Int, modifier: Modifier = Modifier) {
    val ink = Ink(
        line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        orange = MaterialTheme.colorScheme.primary,
        peach = MaterialTheme.expiryColors.peach,
        paper = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(WIDTH / HEIGHT)) {
        val factor = size.width / WIDTH
        scale(factor, factor, pivot = Offset.Zero) {
            wash(ink)
            when (page) {
                0 -> groceries(ink)
                1 -> reminder(ink)
                else -> meal(ink)
            }
        }
    }
}

private val Line = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.wash(ink: Ink) {
    drawOval(ink.peach, topLeft = Offset(30f, 40f), size = Size(230f, 170f))
    drawCircle(ink.peach.copy(alpha = 0.6f), radius = 55f, center = Offset(235f, 75f))
}

private fun DrawScope.sparkle(ink: Ink, x: Float, y: Float) {
    listOf(-14f to -6f, -4f to -16f, 8f to -14f).forEach { (dx, dy) ->
        drawLine(ink.orange, Offset(x, y), Offset(x + dx, y + dy), strokeWidth = 2.6f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.groceries(ink: Ink) {
    // Milk carton.
    drawRect(ink.paper, Offset(80f, 100f), Size(60f, 90f))
    drawRect(ink.line, Offset(80f, 100f), Size(60f, 90f), style = Line)
    val side = Path().apply {
        moveTo(140f, 100f); lineTo(175f, 86f); lineTo(175f, 176f); lineTo(140f, 190f); close()
    }
    drawPath(side, ink.paper)
    drawPath(side, ink.line, style = Line)
    val roof = Path().apply {
        moveTo(80f, 100f); lineTo(104f, 78f); lineTo(160f, 78f); lineTo(175f, 86f)
        moveTo(104f, 78f); lineTo(140f, 100f)
    }
    drawPath(roof, ink.line, style = Line)
    drawRect(ink.line, Offset(112f, 66f), Size(34f, 12f), style = Line)

    // Egg tray with three eggs.
    drawRoundRect(ink.peach, Offset(88f, 188f), Size(96f, 18f), CornerRadius(8f))
    drawRoundRect(ink.line, Offset(88f, 188f), Size(96f, 18f), CornerRadius(8f), style = Line)
    listOf(102f to 176f, 132f to 180f, 162f to 176f).forEach { (cx, cy) ->
        drawOval(ink.paper, Offset(cx - 14f, cy - 18f), Size(28f, 36f))
        drawOval(ink.line, Offset(cx - 14f, cy - 18f), Size(28f, 36f), style = Line)
    }

    // Lettuce leaf.
    val leaf = Path().apply {
        moveTo(205f, 150f)
        cubicTo(180f, 140f, 175f, 100f, 200f, 92f)
        cubicTo(215f, 88f, 235f, 100f, 225f, 125f)
        cubicTo(222f, 138f, 212f, 146f, 205f, 150f)
        close()
    }
    drawPath(leaf, Color(0xFFDDE5B5).copy(alpha = 0.9f))
    drawPath(leaf, ink.line, style = Line)

    // Tomato.
    drawCircle(ink.orange.copy(alpha = 0.55f), 28f, Offset(205f, 176f))
    drawCircle(ink.line, 28f, Offset(205f, 176f), style = Line)
    drawLine(ink.line, Offset(205f, 150f), Offset(196f, 157f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(ink.line, Offset(205f, 150f), Offset(214f, 157f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(ink.line, Offset(205f, 147f), Offset(205f, 158f), strokeWidth = 2.2f, cap = StrokeCap.Round)

    sparkle(ink, 66f, 66f)
}

private fun DrawScope.reminder(ink: Ink) {
    val origin = Offset(70f, 62f)
    val body = Size(170f, 148f)
    drawRoundRect(ink.paper, origin, body, CornerRadius(14f))
    drawRoundRect(ink.peach, origin, Size(170f, 38f), CornerRadius(14f))
    drawRect(ink.peach, Offset(70f, 80f), Size(170f, 20f))
    drawRoundRect(ink.line, origin, body, CornerRadius(14f), style = Line)
    drawLine(ink.line, Offset(70f, 100f), Offset(240f, 100f), strokeWidth = 2.2f)

    // Binder rings.
    listOf(100f, 198f).forEach { x ->
        drawRoundRect(ink.paper, Offset(x, 48f), Size(10f, 26f), CornerRadius(5f))
        drawRoundRect(ink.line, Offset(x, 48f), Size(10f, 26f), CornerRadius(5f), style = Line)
    }

    // Day grid with one highlighted, checked day.
    for (row in 0..2) {
        for (col in 0..3) {
            val topLeft = Offset(88f + col * 36f, 112f + row * 32f)
            val highlighted = row == 1 && col == 2
            drawRoundRect(
                color = if (highlighted) ink.orange else ink.orange.copy(alpha = 0.18f),
                topLeft = topLeft,
                size = Size(26f, 22f),
                cornerRadius = CornerRadius(5f),
            )
            if (highlighted) {
                val tick = Path().apply {
                    moveTo(topLeft.x + 6f, topLeft.y + 11f)
                    lineTo(topLeft.x + 12f, topLeft.y + 17f)
                    lineTo(topLeft.x + 21f, topLeft.y + 6f)
                }
                drawPath(tick, Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    // Bell.
    val bell = Path().apply {
        moveTo(212f, 205f)
        cubicTo(212f, 180f, 218f, 160f, 236f, 160f)
        cubicTo(254f, 160f, 260f, 180f, 260f, 205f)
        close()
    }
    drawPath(bell, ink.orange.copy(alpha = 0.25f))
    drawPath(bell, ink.line, style = Line)
    drawLine(ink.line, Offset(204f, 205f), Offset(268f, 205f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    drawCircle(ink.line, 5f, Offset(236f, 214f), style = Line)

    sparkle(ink, 276f, 150f)
}

private fun DrawScope.meal(ink: Ink) {
    // Bowl.
    val bowl = Path().apply {
        moveTo(70f, 150f); lineTo(230f, 150f)
        cubicTo(230f, 200f, 190f, 222f, 150f, 222f)
        cubicTo(110f, 222f, 70f, 200f, 70f, 150f)
        close()
    }
    drawPath(bowl, Color(0xFFFBD9B5))
    drawPath(bowl, ink.line, style = Line)
    drawOval(ink.paper, Offset(82f, 118f), Size(136f, 44f))
    drawOval(ink.line, Offset(82f, 118f), Size(136f, 44f), style = Line)

    // Steam.
    listOf(120f, 150f, 180f).forEach { x ->
        val steam = Path().apply {
            moveTo(x, 104f)
            cubicTo(x - 9f, 94f, x + 9f, 84f, x, 72f)
        }
        drawPath(steam, ink.line, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }

    // Avocado half.
    drawOval(Color(0xFFCFE3C0), Offset(214f, 120f), Size(62f, 92f))
    drawOval(ink.line, Offset(214f, 120f), Size(62f, 92f), style = Line)
    drawOval(Color(0xFFF6E7B0), Offset(222f, 130f), Size(46f, 72f))
    drawCircle(ink.orange.copy(alpha = 0.55f), 13f, Offset(245f, 172f))
    drawCircle(ink.line, 13f, Offset(245f, 172f), style = Line)

    sparkle(ink, 256f, 98f)
}
