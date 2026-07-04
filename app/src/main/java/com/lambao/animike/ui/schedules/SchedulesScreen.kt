package com.lambao.animike.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.ScheduledAnime
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.PagingGridError
import com.lambao.animike.ui.components.PagingRefreshErrorBanner
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens

// Không có Scaffold/title riêng — đây là nội dung 1 tab bên trong BrowseScreen
// (segmented control "Theo mùa" / "Theo thứ"), Scaffold + title "Duyệt" đã có
// ở BrowseScreen (tránh tiêu thụ insets 2 lần như đã fix ở các screen khác).
@Composable
fun SchedulesScreen(
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchedulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SchedulesEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    SchedulesScreenContent(state = state, pagingItems = pagingItems, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
private fun SchedulesScreenContent(
    state: SchedulesState,
    pagingItems: LazyPagingItems<ScheduledAnime>,
    onEvent: (SchedulesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DayRow(
            selectedDay = state.selectedDay,
            dayDates = state.dayDates,
            onDaySelected = { onEvent(SchedulesEvent.OnDaySelected(it)) },
        )

        // Banner lỗi nhẹ khi refresh (đổi thứ) thất bại nhưng vẫn còn item
        // cũ để hiện — giữ cache cũ hiển thị + báo lỗi nhẹ, không che list
        // bên dưới (jikan-api SKILL.md mục Caching, điểm 3).
        if (pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount > 0) {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            PagingRefreshErrorBanner(
                message = error.toAppError().toUserMessage(),
                onRetry = { pagingItems.retry() },
            )
        }

        when {
            // Không check itemCount == 0: đổi thứ tạo Pager mới nhưng
            // LazyPagingItems vẫn giữ item cũ cho tới khi trang mới tải xong —
            // ưu tiên refresh-loading để tránh hiện nhầm lịch chiếu của thứ trước.
            pagingItems.loadState.refresh is LoadState.Loading -> ScheduleListLoading()

            pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                PagingGridError(
                    message = error.toAppError().toUserMessage(),
                    onRetry = { pagingItems.retry() },
                )
            }

            pagingItems.itemCount == 0 -> EmptyContent()

            else -> ScheduleList(
                pagingItems = pagingItems,
                favoriteMalIds = state.favoriteMalIds,
                onAnimeClick = { onEvent(SchedulesEvent.OnAnimeClick(it)) },
                onFavoriteToggle = { onEvent(SchedulesEvent.OnFavoriteToggle(it)) },
            )
        }
    }
}

@Composable
private fun DayRow(selectedDay: String, dayDates: Map<String, Int>, onDaySelected: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(weekDays, key = { it }) { day ->
            DayChip(
                label = weekDayLabels[day] ?: day,
                date = dayDates[day],
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
            )
        }
    }
}

// Chip 2 dòng (thứ + ngày dương lịch) theo kit Animax "Release Calendar".
@Composable
private fun DayChip(label: String, date: Int?, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (date != null) {
            Text(
                text = "$date",
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ScheduleList(
    pagingItems: LazyPagingItems<ScheduledAnime>,
    favoriteMalIds: Set<Int>,
    onAnimeClick: (Int) -> Unit,
    onFavoriteToggle: (ScheduledAnime) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimens.SpaceSm),
    ) {
        // items(count, key, contentType, itemContent) là member function của
        // LazyListScope — không cần import (giống LazyGridScope.items).
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.malId },
            contentType = pagingItems.itemContentType { "scheduled_anime" },
        ) { index ->
            pagingItems[index]?.let { anime ->
                ScheduleRow(
                    anime = anime,
                    isFavorite = anime.malId in favoriteMalIds,
                    onClick = { onAnimeClick(anime.malId) },
                    onFavoriteClick = { onFavoriteToggle(anime) },
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                val shimmerProgress = rememberShimmerProgress()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm)
                        .height(Dimens.ScheduleRowHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusCard))
                        .shimmerEffect(shimmerProgress),
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceMd),
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
private fun ScheduleRow(
    anime: ScheduledAnime,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        AsyncImage(
            model = anime.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .width(Dimens.ScheduleThumbnailWidth)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(Dimens.RadiusCard)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = scheduleMetaLine(anime)
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FavoriteChip(isFavorite = isFavorite, onClick = onFavoriteClick)
        }
    }
}

private fun scheduleMetaLine(anime: ScheduledAnime): String = listOfNotNull(
    anime.episodes?.let { "Tập $it" },
    anime.broadcastTime,
).joinToString(" · ")

// Filled (primary) khi CHƯA thêm, outline khi ĐÃ thêm — bố cục theo kit Animax
// (docs/UI/22_Dark_top hits anime.png: card 1 "+ My List" filled, card 2 "✓ My
// List" outline), nhưng màu trạng thái "đã thêm" dùng `secondary` để khớp quy
// ước chung của app (animike-design SKILL.md: "Nút favorite... active = secondary",
// xem DetailScreen.FavoriteButton dùng secondary cho icon trái tim active).
@Composable
private fun FavoriteChip(isFavorite: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.RadiusChip)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (isFavorite) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, shape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = if (isFavorite) "✓ Yêu thích" else "+ Yêu thích",
            style = MaterialTheme.typography.labelSmall,
            color = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ScheduleListLoading() {
    val shimmerProgress = rememberShimmerProgress()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ScheduleRowHeight)
                    .clip(RoundedCornerShape(Dimens.RadiusCard))
                    .shimmerEffect(shimmerProgress),
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có anime nào chiếu vào ngày này",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
