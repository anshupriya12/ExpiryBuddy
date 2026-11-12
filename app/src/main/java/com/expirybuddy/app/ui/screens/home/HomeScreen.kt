package com.expirybuddy.app.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.expiry.ClassifiedItem
import com.expirybuddy.app.domain.expiry.DayPeriod
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.AddButton
import com.expirybuddy.app.ui.components.AppIcons
import com.expirybuddy.app.ui.components.AppSnackbarHost
import com.expirybuddy.app.ui.components.EmptyState
import com.expirybuddy.app.ui.components.ExpiryBuddySearchField
import com.expirybuddy.app.ui.components.ItemCard
import com.expirybuddy.app.ui.components.MessageEffect
import com.expirybuddy.app.ui.components.WashCorner
import com.expirybuddy.app.ui.components.WashedIcon
import com.expirybuddy.app.ui.components.peachWash
import com.expirybuddy.app.ui.theme.HandwrittenStyle
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors
import com.expirybuddy.app.ui.util.statusLabel
import java.time.LocalTime

@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onSeeAll: () -> Unit,
    snackbarHostState: SnackbarHostState,
    @StringRes message: Int?,
    onMessageShown: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var period by remember { mutableStateOf(DayPeriod.forHour(LocalTime.now().hour)) }

    // Statuses and the greeting depend on the time, which may have changed while the app was away.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        period = DayPeriod.forHour(LocalTime.now().hour)
        onPauseOrDispose { }
    }
    MessageEffect(message, snackbarHostState, onMessageShown)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .peachWash(WashCorner.BottomStart),
    ) {
        Scaffold(
            // The whole screen rises above the keyboard so the search field stays visible while typing.
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { AppSnackbarHost(snackbarHostState) },
            bottomBar = {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onAddItem = onAddItem,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.page,
                    end = Spacing.page,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(key = "header") { Header(period, onOpenSettings) }
                when (val current = state) {
                    HomeUiState.Loading -> item(key = "loading") { Spacer(Modifier.height(240.dp)) }
                    HomeUiState.Error -> item(key = "error") {
                        EmptyState(
                            body = stringResource(R.string.home_error),
                            modifier = Modifier.padding(vertical = Spacing.xl),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = viewModel::retry,
                        )
                    }
                    is HomeUiState.Content -> content(
                        current = current,
                        onAddItem = onAddItem,
                        onOpenItem = onOpenItem,
                        onSeeAll = onSeeAll,
                        onClearFilters = viewModel::onClearFilters,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.content(
    current: HomeUiState.Content,
    onAddItem: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val sections = current.sections
    when {
        current.totalCount == 0 -> item(key = "empty") {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                body = stringResource(R.string.home_empty_body),
                modifier = Modifier.padding(vertical = Spacing.xl),
                illustration = {
                    WashedIcon(
                        icon = AppIcons.Box,
                        tint = MaterialTheme.colorScheme.primary,
                        wash = MaterialTheme.expiryColors.peach,
                    )
                },
                tagline = stringResource(R.string.home_empty_tagline),
                taglineStyle = HandwrittenStyle,
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onAddItem,
            )
        }

        // Searching: show every matching active item, not just the expiring ones.
        current.filter.isActive -> {
            val results = sections.expired + sections.expiringSoon + sections.upcoming
            if (results.isEmpty()) {
                item(key = "no-results") {
                    EmptyState(
                        title = stringResource(R.string.no_results_title),
                        body = stringResource(R.string.no_results_body),
                        modifier = Modifier.padding(vertical = Spacing.xl),
                        actionLabel = stringResource(R.string.no_results_action),
                        onAction = onClearFilters,
                    )
                }
            } else {
                item(key = "results-header") {
                    SectionTitle(stringResource(R.string.home_search_results, results.size))
                }
                cards(results, onOpenItem)
            }
        }

        else -> {
            if (sections.expired.isNotEmpty()) {
                item(key = "expired-header") {
                    SectionTitle(stringResource(R.string.home_section_title, statusLabel(ExpiryStatus.EXPIRED), sections.expired.size))
                }
                cards(sections.expired, onOpenItem)
            }
            if (sections.expiringSoon.isNotEmpty()) {
                item(key = "soon-header") {
                    SectionTitle(
                        title = stringResource(R.string.home_section_title, statusLabel(ExpiryStatus.EXPIRING_SOON), sections.expiringSoon.size),
                        action = { SeeAll(onSeeAll) },
                    )
                }
                cards(sections.expiringSoon, onOpenItem)
            } else if (sections.expired.isEmpty()) {
                item(key = "nothing-soon") {
                    EmptyState(
                        title = stringResource(R.string.home_nothing_soon_title),
                        body = stringResource(R.string.home_nothing_soon_body),
                        modifier = Modifier.padding(vertical = Spacing.xl),
                        illustration = {
                            WashedIcon(
                                icon = Icons.Outlined.CheckCircle,
                                tint = MaterialTheme.expiryColors.success,
                                wash = MaterialTheme.expiryColors.successContainer,
                            )
                        },
                        actionLabel = stringResource(R.string.home_nothing_soon_action),
                        onAction = onSeeAll,
                    )
                }
            } else {
                item(key = "see-all-only") { SectionTitle(title = "", action = { SeeAll(onSeeAll) }) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.cards(
    list: List<ClassifiedItem>,
    onOpenItem: (Long) -> Unit,
) {
    items(list, key = { it.item.id }) { classified ->
        ItemCard(classified, onClick = { onOpenItem(classified.item.id) })
    }
}

@Composable
private fun Header(period: DayPeriod, onOpenSettings: () -> Unit) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(Spacing.touchTarget)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(
                when (period) {
                    DayPeriod.MORNING -> R.string.home_greeting_morning
                    DayPeriod.AFTERNOON -> R.string.home_greeting_afternoon
                    DayPeriod.EVENING -> R.string.home_greeting_evening
                },
            ),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun SectionTitle(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        action?.invoke()
    }
}

@Composable
private fun SeeAll(onClick: () -> Unit) {
    val description = stringResource(R.string.home_see_all_description)
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = Spacing.touchTarget)
            .semantics { contentDescription = description },
    ) {
        Text(stringResource(R.string.home_see_all), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.size(Spacing.xs))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/** Search field and the round add button, pinned to the bottom for one-handed reach. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddItem: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.page, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ExpiryBuddySearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f),
        )
        AddButton(contentDescription = stringResource(R.string.nav_add_item), onClick = onAddItem)
    }
}
