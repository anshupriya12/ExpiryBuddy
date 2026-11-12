package com.expirybuddy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.expirybuddy.app.R
import com.expirybuddy.app.ui.theme.expiryColors

/** Which corner the soft peach wash sits in. Purely decorative, low contrast and behind all content. */
enum class WashCorner { TopEnd, BottomStart }

@Composable
fun Modifier.peachWash(corner: WashCorner): Modifier {
    val color = MaterialTheme.expiryColors.peach
    return drawBehind {
        val base = size.minDimension
        val (a, b) = when (corner) {
            WashCorner.TopEnd -> Offset(size.width * 0.98f, base * 0.02f) to Offset(size.width * 0.78f, -base * 0.06f)
            WashCorner.BottomStart -> Offset(size.width * 0.02f, size.height * 0.99f) to Offset(size.width * 0.22f, size.height * 1.06f)
        }
        drawCircle(color = color.copy(alpha = 0.9f), radius = base * 0.34f, center = a)
        drawCircle(color = color.copy(alpha = 0.6f), radius = base * 0.26f, center = b)
    }
}

/** App bar used by every inner screen: Playfair title, a back arrow and optional actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryBuddyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * Shared screen frame: canvas background with the peach wash, the top bar, an optional bottom bar and a
 * snackbar host. Insets are handled by [Scaffold] (the activity draws edge to edge).
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    washCorner: WashCorner? = WashCorner.TopEnd,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .let { if (washCorner != null) it.peachWash(washCorner) else it },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { ExpiryBuddyTopBar(title, onNavigateBack = onNavigateBack, actions = actions) },
            bottomBar = bottomBar,
            snackbarHost = { if (snackbarHostState != null) AppSnackbarHost(snackbarHostState) },
            content = content,
        )
    }
}
