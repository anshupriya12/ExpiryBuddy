package com.expirybuddy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.ui.theme.expiryColors
import com.expirybuddy.app.ui.util.expiryCountdownText

/** The colour that goes with a status. Always shown together with words ("Expires in 2 days"). */
@Composable
fun expiryStatusColor(status: ExpiryStatus): Color = when (status) {
    ExpiryStatus.EXPIRED -> MaterialTheme.expiryColors.expired
    ExpiryStatus.EXPIRING_SOON -> MaterialTheme.expiryColors.soon
    ExpiryStatus.UPCOMING -> MaterialTheme.expiryColors.upcoming
}

/**
 * Relative expiry text such as "Expires in 2 days" or "Expired 3 days ago". The wording carries the
 * meaning; the colour only reinforces it, so the status never depends on colour alone.
 */
@Composable
fun ExpiryStatusLabel(
    status: ExpiryStatus,
    daysUntilExpiry: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text = expiryCountdownText(daysUntilExpiry),
        modifier = modifier,
        style = style,
        fontWeight = FontWeight.Medium,
        color = expiryStatusColor(status),
    )
}
