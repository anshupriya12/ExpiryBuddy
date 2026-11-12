package com.expirybuddy.app.ui.screens.all

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.expiry.ItemSort
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.AppIcons
import com.expirybuddy.app.ui.components.CategoryFilterChip
import com.expirybuddy.app.ui.components.EmptyState
import com.expirybuddy.app.ui.components.ExpiryBuddySearchField
import com.expirybuddy.app.ui.components.ItemCard
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageEffect
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.components.WashedIcon
import com.expirybuddy.app.ui.components.categoryIcon
import com.expirybuddy.app.ui.theme.Spacing
import com.expirybuddy.app.ui.theme.expiryColors
import com.expirybuddy.app.ui.util.categoryLabel

/** The complete inventory: search, category filters, a Used filter and sorting. */
@Composable
fun AllItemsScreen(
    onNavigateBack: () -> Unit,
    onOpenItem: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    @StringRes message: Int?,
    onMessageShown: () -> Unit,
    viewModel: AllItemsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    // Statuses depend on today's date, which may have changed while the app was in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }
    MessageEffect(message, snackbarHostState, onMessageShown)

    ScreenScaffold(
        title = stringResource(R.string.all_items_title),
        // The list area shrinks above the keyboard so results stay reachable while typing.
        modifier = Modifier.imePadding(),
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ExpiryBuddySearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.page),
            )
            val content = state as? AllItemsUiState.Content
            Filters(
                selectedCategory = content?.filter?.category,
                showUsed = content?.showUsed == true,
                hasUsedItems = content?.hasUsedItems == true,
                onCategorySelected = viewModel::onCategorySelected,
                onShowUsedChange = viewModel::onShowUsedChange,
            )
            if (content != null && !content.showUsed) {
                SortRow(sort = content.sort, onSortSelected = viewModel::onSortSelected)
            }
            when (val current = state) {
                AllItemsUiState.Loading -> LoadingState()
                AllItemsUiState.Error -> EmptyState(
                    body = stringResource(R.string.home_error),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = viewModel::retry,
                )
                is AllItemsUiState.Content -> ItemsList(
                    content = current,
                    onOpenItem = onOpenItem,
                    onClearFilters = viewModel::onClearFilters,
                )
            }
        }
    }
}

@Composable
private fun Filters(
    selectedCategory: ItemCategory?,
    showUsed: Boolean,
    hasUsedItems: Boolean,
    onCategorySelected: (ItemCategory?) -> Unit,
    onShowUsedChange: (Boolean) -> Unit,
) {
    val description = stringResource(R.string.filter_by_category)
    LazyRow(
        modifier = Modifier
            .padding(top = Spacing.sm)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = Spacing.page),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            CategoryFilterChip(
                label = stringResource(R.string.filter_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(ItemCategory.entries, key = { it.storageKey }) { category ->
            CategoryFilterChip(
                label = categoryLabel(category),
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                icon = categoryIcon(category),
            )
        }
        // Items marked as used are kept; this chip is the way back to them.
        if (hasUsedItems || showUsed) {
            item(key = "used") {
                CategoryFilterChip(
                    label = stringResource(R.string.filter_used),
                    selected = showUsed,
                    onClick = { onShowUsedChange(!showUsed) },
                    icon = Icons.Outlined.Check,
                )
            }
        }
    }
}

@Composable
private fun SortRow(sort: ItemSort, onSortSelected: (ItemSort) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.page),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                Icon(AppIcons.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = stringResource(R.string.sort_label, sortLabel(sort)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Text(
                    text = stringResource(R.string.sort_menu_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
                ItemSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(option)) },
                        trailingIcon = {
                            if (option == sort) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSortSelected(option)
                        },
                        modifier = Modifier.heightIn(min = Spacing.touchTarget),
                    )
                }
            }
        }
    }
}

@Composable
private fun sortLabel(sort: ItemSort): String = stringResource(
    when (sort) {
        ItemSort.EXPIRY_DATE -> R.string.sort_expiry_date
        ItemSort.NAME -> R.string.sort_name
        ItemSort.CATEGORY -> R.string.sort_category
    },
)

@Composable
private fun ItemsList(
    content: AllItemsUiState.Content,
    onOpenItem: (Long) -> Unit,
    onClearFilters: () -> Unit,
) {
    when {
        content.modeTotal == 0 -> EmptyState(
            title = stringResource(if (content.showUsed) R.string.all_items_used_empty_title else R.string.all_items_empty_title),
            body = stringResource(if (content.showUsed) R.string.all_items_used_empty_body else R.string.all_items_empty_body),
            illustration = {
                WashedIcon(AppIcons.Box, MaterialTheme.colorScheme.primary, MaterialTheme.expiryColors.peach)
            },
        )
        content.items.isEmpty() -> EmptyState(
            title = stringResource(R.string.no_results_title),
            body = stringResource(R.string.no_results_body),
            actionLabel = stringResource(R.string.no_results_action),
            onAction = onClearFilters,
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(content.items, key = { it.item.id }) { classified ->
                ItemCard(classified, onClick = { onOpenItem(classified.item.id) }, compact = true)
            }
        }
    }
}
