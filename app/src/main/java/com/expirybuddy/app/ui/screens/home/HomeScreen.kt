package com.expirybuddy.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expirybuddy.app.R
import com.expirybuddy.app.domain.expiry.ClassifiedItem
import com.expirybuddy.app.domain.expiry.ExpiryStatus
import com.expirybuddy.app.domain.expiry.HomeSections
import com.expirybuddy.app.domain.expiry.StatusCounts
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.ui.AppViewModelProvider
import com.expirybuddy.app.ui.components.LoadingState
import com.expirybuddy.app.ui.components.MessageState
import com.expirybuddy.app.ui.components.ScreenScaffold
import com.expirybuddy.app.ui.util.categoryLabel
import com.expirybuddy.app.ui.util.expiryCountdownText
import com.expirybuddy.app.ui.util.formatDate
import com.expirybuddy.app.ui.util.statusLabel

@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenItem: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    // Statuses depend on today's date, which may have changed while the app was in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }

    ScreenScaffold(
        title = stringResource(R.string.home_title),
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.nav_add_item),
                )
            }
        },
    ) { padding ->
        // consumeWindowInsets lets the imePadding in ItemsContent add only the keyboard's extra height.
        val contentModifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (val current = state) {
            HomeUiState.Loading -> LoadingState(contentModifier)
            HomeUiState.Error -> MessageState(
                body = stringResource(R.string.home_error),
                modifier = contentModifier,
                actionLabel = stringResource(R.string.action_retry),
                onAction = viewModel::retry,
            )
            is HomeUiState.Content -> if (current.totalCount == 0) {
                MessageState(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    modifier = contentModifier,
                    actionLabel = stringResource(R.string.home_empty_action),
                    onAction = onAddItem,
                )
            } else {
                ItemsContent(
                    content = current,
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onCategorySelected = viewModel::onCategorySelected,
                    onClearFilters = viewModel::onClearFilters,
                    onOpenItem = onOpenItem,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun ItemsContent(
    content: HomeUiState.Content,
    query: String,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (ItemCategory?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // imePadding shrinks the list/message area when the keyboard opens, so results stay reachable.
    Column(modifier = modifier.imePadding()) {
        // Search and category chips stay pinned above the scrolling list.
        SearchAndFilters(
            query = query,
            selectedCategory = content.filter.category,
            onQueryChange = onQueryChange,
            onCategorySelected = onCategorySelected,
        )
        if (content.sections.isEmpty) {
            MessageState(
                title = stringResource(R.string.no_results_title),
                body = stringResource(R.string.no_results_body),
                actionLabel = stringResource(R.string.no_results_action),
                onAction = onClearFilters,
            )
        } else {
            ItemList(content.sections, onOpenItem)
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    selectedCategory: ItemCategory?,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (ItemCategory?) -> Unit,
) {
    val focusManager: FocusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        val filterDescription = stringResource(R.string.filter_by_category)
        LazyRow(
            modifier = Modifier.semantics { contentDescription = filterDescription },
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
            }
            items(ItemCategory.entries, key = { it.storageKey }) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(categoryLabel(category)) },
                )
            }
        }
    }
}

@Composable
private fun ItemList(sections: HomeSections, onOpenItem: (Long) -> Unit) {
    val groups = listOf(
        ExpiryStatus.EXPIRED to sections.expired,
        ExpiryStatus.EXPIRING_SOON to sections.expiringSoon,
        ExpiryStatus.UPCOMING to sections.upcoming,
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        // Extra bottom space keeps the last row clear of the floating action button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "summary") { SummaryRow(sections.counts) }
        groups.forEach { (status, list) ->
            if (list.isNotEmpty()) {
                item(key = "header-${status.name}") { SectionHeader(status, list.size) }
                items(list, key = { it.item.id }) { classified ->
                    ItemRow(classified, onClick = { onOpenItem(classified.item.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(counts: StatusCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard(
            count = counts.expired,
            label = statusLabel(ExpiryStatus.EXPIRED),
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            count = counts.expiringSoon,
            label = statusLabel(ExpiryStatus.EXPIRING_SOON),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            count = counts.upcoming,
            label = statusLabel(ExpiryStatus.UPCOMING),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.home_count_description, count, label)
    Card(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionHeader(status: ExpiryStatus, count: Int) {
    Text(
        text = stringResource(R.string.home_section_title, statusLabel(status), count),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ItemRow(classified: ClassifiedItem, onClick: () -> Unit) {
    val item = classified.item
    val category = categoryLabel(item.category)
    val date = formatDate(item.expiryDate)
    val countdown = expiryCountdownText(classified.daysUntilExpiry)
    val description = stringResource(R.string.home_item_description, item.name, category, countdown, date)
    val expired = classified.status == ExpiryStatus.EXPIRED

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        // The card's merged description above replaces the individual texts for screen readers.
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                text = stringResource(R.string.home_item_category_date, category, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = countdown,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
