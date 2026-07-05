package com.lambao.animike.ui.communityrecommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.CommunityRecommendation
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.CommunityRecommendationCard
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens

@Composable
fun CommunityRecommendationsScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: CommunityRecommendationsViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CommunityRecommendationsEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    CommunityRecommendationsScreenContent(
        pagingItems = pagingItems,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun CommunityRecommendationsScreenContent(
    pagingItems: LazyPagingItems<CommunityRecommendation>,
    onBackClick: () -> Unit,
    onEvent: (CommunityRecommendationsEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Đề xuất cộng đồng",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                pagingItems.loadState.refresh is LoadState.Loading -> RecommendationsListLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    RecommendationsErrorContent(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> RecommendationsEmptyContent()

                else -> RecommendationsPagingList(
                    pagingItems = pagingItems,
                    onAnimeClick = { onEvent(CommunityRecommendationsEvent.OnAnimeClick(it)) },
                )
            }
        }
    }
}

@Composable
private fun RecommendationsPagingList(
    pagingItems: LazyPagingItems<CommunityRecommendation>,
    onAnimeClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "communityRecommendation" },
        ) { index ->
            pagingItems[index]?.let { recommendation ->
                CommunityRecommendationCard(
                    recommendation = recommendation,
                    onAnimeClick = onAnimeClick,
                    modifier = Modifier.fillMaxWidth(),
                    // Nhiều dòng hơn preview Home (2) vì fillMaxWidth có nhiều
                    // chỗ dọc hơn width cố định trong LazyRow.
                    contentMaxLines = 5,
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                val shimmerProgress = rememberShimmerProgress()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ReviewCardPlaceholderHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusCard))
                        .shimmerEffect(shimmerProgress),
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { pagingItems.retry() }) {
                        Text("Tải thêm thất bại — Thử lại")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsListLoading() {
    val shimmerProgress = rememberShimmerProgress()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ReviewCardPlaceholderHeight)
                    .clip(RoundedCornerShape(Dimens.RadiusCard))
                    .shimmerEffect(shimmerProgress),
            )
        }
    }
}

@Composable
private fun RecommendationsErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(text = "(￣ヘ￣)", style = MaterialTheme.typography.displaySmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
private fun RecommendationsEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Chưa có đề xuất nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
