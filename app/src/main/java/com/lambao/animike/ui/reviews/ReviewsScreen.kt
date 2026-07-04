package com.lambao.animike.ui.reviews

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens

@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    ReviewsScreenContent(pagingItems = pagingItems, onBackClick = onBackClick)
}

@Composable
private fun ReviewsScreenContent(
    pagingItems: LazyPagingItems<AnimeReview>,
    onBackClick: () -> Unit,
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
                    text = "Đánh giá",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                // Không check itemCount == 0: giống Episodes/Schedules — ưu
                // tiên refresh-loading, dù ở đây malId cố định không có
                // filter/selection để tạo lại Pager giữa chừng.
                pagingItems.loadState.refresh is LoadState.Loading -> ReviewsListLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    ReviewsErrorContent(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> ReviewsEmptyContent()

                else -> ReviewsPagingList(pagingItems = pagingItems)
            }
        }
    }
}

@Composable
private fun ReviewsPagingList(pagingItems: LazyPagingItems<AnimeReview>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        // items(count, key, contentType, itemContent) là member function của
        // LazyListScope — không cần import (giống pattern ScheduleList).
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "review" },
        ) { index ->
            pagingItems[index]?.let { review -> ReviewCard(review) }
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

// Trùng với ReviewCard private trong DetailScreen.kt — cố ý duplicate (quy
// ước dự án: chỉ tách shared component khi có từ 3 nơi dùng trở lên), không
// phải thiếu sót.
@Composable
private fun ReviewCard(review: AnimeReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = review.username,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (review.score != null) {
                Text(
                    text = "★ ${review.score}/10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(
            text = review.reviewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReviewsListLoading() {
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
private fun ReviewsErrorContent(message: String, onRetry: () -> Unit) {
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
private fun ReviewsEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có đánh giá nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
