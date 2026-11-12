package com.expirybuddy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.expiry.ClassifiedItem
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.expiryCountdownText
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.formatInstantDate

/**
 * One item in a list: thumbnail (photo or category icon), name, "Category · date", relative expiry and a
 * chevron. The whole card is one tap target and is read out as a single sentence by screen readers.
 *
 * @param compact smaller thumbnail and padding, used in the All Items list.
 */
@Composable
fun ItemCard(
    classified: ClassifiedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val item = classified.item
    val category = categoryLabel(item.category)
    val date = formatDate(item.expiryDate)
    val usedText = item.usedAt?.let { stringResource(R.string.status_used_on, formatInstantDate(it)) }
    val statusText = usedText ?: expiryCountdownText(classified.daysUntilExpiry)
    val description = stringResource(R.string.home_item_description, item.name, category, statusText, date)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        // The merged description above replaces the individual texts for screen readers.
        Row(
            modifier = Modifier
                .heightIn(min = Spacing.touchTarget)
                .padding(if (compact) Spacing.md else Spacing.md)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ItemThumbnail(item.photoUri, item.category, size = if (compact) 56.dp else 64.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.home_item_category_date, category, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (usedText != null) {
                    Text(
                        text = usedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.expiryColors.success,
                    )
                } else {
                    ExpiryStatusLabel(classified.status, classified.daysUntilExpiry)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
