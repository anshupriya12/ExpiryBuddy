package com.expirybuddy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors

/**
 * Optional photo control. With no photo it is a large "Add a photo" card; with one it shows a preview
 * with Replace and Remove. Choosing a source (camera or gallery) happens in a bottom sheet.
 */
@Composable
fun PhotoPicker(
    photoUri: String?,
    isBusy: Boolean,
    failed: Boolean,
    canTakePhoto: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val busyDescription = stringResource(R.string.photo_busy)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (photoUri == null) {
            Surface(
                onClick = { showSheet = true },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 148.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.expiryColors.peach,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = busyDescription },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.padding(Spacing.lg),
                        ) {
                            Icon(
                                AppIcons.Camera,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(stringResource(R.string.photo_add), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.photo_optional),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            Box {
                ItemPhoto(
                    photoUri = photoUri,
                    contentDescription = stringResource(R.string.photo_preview_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large),
                )
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { contentDescription = busyDescription },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(
                    onClick = { showSheet = true },
                    enabled = !isBusy,
                    modifier = Modifier.heightIn(min = Spacing.touchTarget),
                ) { Text(stringResource(R.string.photo_replace)) }
                TextButton(
                    onClick = onRemove,
                    enabled = !isBusy,
                    modifier = Modifier.heightIn(min = Spacing.touchTarget),
                ) { Text(stringResource(R.string.photo_remove)) }
            }
        }

        if (failed) {
            Text(
                text = stringResource(R.string.photo_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showSheet) {
        PhotoSourceSheet(
            canTakePhoto = canTakePhoto,
            onTakePhoto = {
                showSheet = false
                onTakePhoto()
            },
            onChooseFromGallery = {
                showSheet = false
                onChooseFromGallery()
            },
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    canTakePhoto: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = Spacing.lg)) {
            Text(
                text = stringResource(R.string.photo_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.sm),
            )
            if (canTakePhoto) {
                SheetAction(AppIcons.Camera, stringResource(R.string.photo_take), onTakePhoto)
            }
            SheetAction(AppIcons.Gallery, stringResource(R.string.photo_gallery), onChooseFromGallery)
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.page),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start)
    }
}
