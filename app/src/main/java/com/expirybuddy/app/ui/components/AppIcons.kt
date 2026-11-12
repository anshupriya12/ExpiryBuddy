package com.expirybuddy.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.domain.model.ItemCategory

/**
 * Thin-stroke outlined icons that Material's core icon set does not have. All use the same 24dp grid,
 * stroke weight and round caps so they sit next to `Icons.Outlined` without looking mismatched.
 * Colour comes from the `Icon` tint, so the stroke colour here is irrelevant.
 */
object AppIcons {
    private fun outlined(name: String, vararg paths: String, strokeWidth: Float = 1.6f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Food: ImageVector by lazy {
        outlined(
            "Food",
            "M7 3v6a2.5 2.5 0 0 0 5 0V3",
            "M9.5 11.5V21",
            "M17 21V3c-2.2 1.4-3.5 4-3.5 7 0 1.6 1.2 2.6 3.5 2.6",
        )
    }

    val Medication: ImageVector by lazy {
        outlined(
            "Medication",
            "M10.5 20.5l-7-7a4.95 4.95 0 0 1 7-7l7 7a4.95 4.95 0 0 1-7 7z",
            "M8.5 8.5l7 7",
        )
    }

    val Cosmetics: ImageVector by lazy {
        outlined(
            "Cosmetics",
            "M9.5 3h5v3h-5z",
            "M8.5 6h7a1 1 0 0 1 1 1v12.5a1.5 1.5 0 0 1-1.5 1.5H9a1.5 1.5 0 0 1-1.5-1.5V7a1 1 0 0 1 1-1z",
        )
    }

    val Document: ImageVector by lazy {
        outlined("Document", "M6 3h8l4 4v14H6z", "M14 3v4h4", "M9 12h6", "M9 16h6")
    }

    val Other: ImageVector by lazy {
        outlined("Other", "M6 12h.01", "M12 12h.01", "M18 12h.01", strokeWidth = 2.6f)
    }

    val Camera: ImageVector by lazy {
        outlined(
            "Camera",
            "M4 8a2 2 0 0 1 2-2h1.5l1.2-2h6.6l1.2 2H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z",
            "M12 17a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z",
        )
    }

    val Gallery: ImageVector by lazy {
        outlined("Gallery", "M4 5h16v14H4z", "M4 16l5-5 4 4 3-3 4 4", "M9 9h.01", strokeWidth = 1.8f)
    }

    val Phone: ImageVector by lazy {
        outlined("Phone", "M8 3h8a1.5 1.5 0 0 1 1.5 1.5v15A1.5 1.5 0 0 1 16 21H8a1.5 1.5 0 0 1-1.5-1.5v-15A1.5 1.5 0 0 1 8 3z", "M11 18h2")
    }

    val Sort: ImageVector by lazy {
        outlined("Sort", "M4 7h9", "M4 12h6", "M4 17h3", "M17 5v14", "M14 16l3 3 3-3")
    }

    val Box: ImageVector by lazy {
        outlined("Box", "M3 8l9-5 9 5v8l-9 5-9-5z", "M3 8l9 5 9-5", "M12 13v8")
    }
}

/** The thin outlined icon for a category, used on item thumbnails and in the category menu. */
fun categoryIcon(category: ItemCategory): ImageVector = when (category) {
    ItemCategory.FOOD -> AppIcons.Food
    ItemCategory.MEDICATION -> AppIcons.Medication
    ItemCategory.COSMETICS -> AppIcons.Cosmetics
    ItemCategory.OTHER -> AppIcons.Other
}
