package com.expirybuddy.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.R
import com.expirybuddy.app.ui.components.PrimaryButton
import com.expirybuddy.app.ui.components.WashCorner
import com.expirybuddy.app.ui.components.peachWash
import com.expirybuddy.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: Int, val body: Int)

private val Pages = listOf(
    OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_body),
    OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_body),
    OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/**
 * First-launch introduction: three swipeable pages, Skip, Next and Get started. It needs no account;
 * finishing or skipping calls [onFinished], which records it so it is not shown again.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == Pages.lastIndex

    // Back steps to the previous page; on the first page it leaves the app as usual.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .peachWash(WashCorner.TopEnd)
            .safeDrawingPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .heightIn(min = Spacing.touchTarget)
                        .padding(end = Spacing.sm),
                ) { Text(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.titleSmall) }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = Pages[index]
            // The page scrolls if text is large, but is centred when it fits, so there is no dead space.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = Spacing.page),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
                ) {
                    OnboardingIllustration(page = index)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(page.title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val indicatorDescription = stringResource(
            R.string.onboarding_page_indicator,
            pagerState.currentPage + 1,
            Pages.size,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg)
                .clearAndSetSemantics { contentDescription = indicatorDescription },
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(Pages.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }

        PrimaryButton(
            text = stringResource(if (isLast) R.string.onboarding_get_started else R.string.onboarding_next),
            onClick = {
                if (isLast) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            trailingIcon = if (isLast) null else Icons.AutoMirrored.Outlined.ArrowForward,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.xl),
        )
    }
}
