package com.lambao.animike.ui.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Episode
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens

@Composable
fun EpisodesScreen(
    onBackClick: () -> Unit,
    viewModel: EpisodesViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    EpisodesScreenContent(pagingItems = pagingItems, onBackClick = onBackClick)
}

@Composable
private fun EpisodesScreenContent(
    pagingItems: LazyPagingItems<Episode>,
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
                    text = "Các tập",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                // Không check itemCount == 0: mở màn lần đầu, refresh luôn ở
                // trạng thái Loading và itemCount == 0 cùng lúc nên không khác
                // biệt — giữ nhất quán với Search/Schedules/SeasonArchive.
                pagingItems.loadState.refresh is LoadState.Loading -> EpisodesGridLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    EpisodesErrorContent(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> EpisodesEmptyContent()

                else -> EpisodesGrid(pagingItems = pagingItems)
            }
        }
    }
}

@Composable
private fun EpisodesGrid(pagingItems: LazyPagingItems<Episode>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        // items(count, key, span, contentType, itemContent) là member function
        // của LazyGridScope — không cần import (giống AnimePagingGrid).
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.number },
            contentType = pagingItems.itemContentType { "episode" },
        ) { index ->
            pagingItems[index]?.let { episode -> EpisodeGridItem(episode) }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val shimmerProgress = rememberShimmerProgress()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.SpaceSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(Dimens.RadiusCard))
                                .shimmerEffect(shimmerProgress),
                        )
                    }
                }
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceMd),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { pagingItems.retry() }) {
                        Text("Tải thêm thất bại — Thử lại")
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeGridItem(episode: Episode) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Dimens.RadiusCard)),
        )
        Text(
            text = "Tập ${episode.number}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EpisodesGridLoading() {
    val shimmerProgress = rememberShimmerProgress()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(count = 8, key = { it }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(Dimens.RadiusCard))
                    .shimmerEffect(shimmerProgress),
            )
        }
    }
}

@Composable
private fun EpisodesErrorContent(message: String, onRetry: () -> Unit) {
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
private fun EpisodesEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có tập nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
