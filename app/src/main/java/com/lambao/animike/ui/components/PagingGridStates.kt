package com.lambao.animike.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.theme.Dimens

private const val PLACEHOLDER_COUNT = 9

/**
 * Grid 3 cột dùng chung cho mọi màn hình Paging 3 anime (Search, Season
 * Archive, Schedules) — tránh trôi dạt code (đã thấy shimmer vs spinner
 * khác nhau giữa Search/SeasonArchive khi mỗi nơi tự viết append-loading).
 */
@Composable
fun AnimePagingGrid(pagingItems: LazyPagingItems<Anime>, onAnimeClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        // items(count, key, span, contentType, itemContent) là member function
        // của LazyGridScope — không cần import (giống item()/align()/weight()).
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.malId },
            contentType = pagingItems.itemContentType { "anime" },
        ) { index ->
            pagingItems[index]?.let { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.malId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                    repeat(3) {
                        AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.weight(1f))
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

/**
 * Shimmer grid dùng cho lần tải đầu VÀ mỗi lần refresh (đổi filter/năm/mùa
 * tạo PagingSource mới) — thay CircularProgressIndicator giữa màn hình để
 * đúng quy ước animike-design (loading = shimmer, không spinner toàn màn
 * trừ trường hợp không thể ước lượng bố cục).
 */
@Composable
fun PagingGridLoading() {
    val shimmerProgress = rememberShimmerProgress()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(PLACEHOLDER_COUNT, key = { it }) {
            AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PagingGridError(message: String, onRetry: () -> Unit) {
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

/**
 * Banner lỗi nhẹ hiển thị PHÍA TRÊN kết quả cũ khi refresh (đổi filter/năm/
 * mùa) thất bại nhưng vẫn còn item cũ để hiện — giữ cache cũ + báo lỗi nhẹ
 * (jikan-api SKILL.md mục Caching, điểm 3), không che mất nội dung đã có.
 */
@Composable
fun PagingRefreshErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text("Thử lại")
        }
    }
}
