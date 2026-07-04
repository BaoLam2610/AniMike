package com.lambao.animike.ui.seasonarchive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.SeasonYear
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.PagingGridError
import com.lambao.animike.ui.components.PagingGridLoading
import com.lambao.animike.ui.components.PagingRefreshErrorBanner
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.Dimens

private val seasonLabels = mapOf(
    "winter" to "Đông",
    "spring" to "Xuân",
    "summer" to "Hè",
    "fall" to "Thu",
)

@Composable
fun SeasonArchiveScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: SeasonArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SeasonArchiveEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    SeasonArchiveScreenContent(state = state, pagingItems = pagingItems, onEvent = viewModel::onEvent)
}

@Composable
private fun SeasonArchiveScreenContent(
    state: SeasonArchiveState,
    pagingItems: LazyPagingItems<Anime>,
    onEvent: (SeasonArchiveEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets cho bottom nav — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = "Kho lưu trữ mùa",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )

            YearRow(
                years = state.years,
                selectedYear = state.selectedYear,
                onYearSelected = { onEvent(SeasonArchiveEvent.OnYearSelected(it)) },
            )
            SeasonRow(
                availableSeasons = state.years.find { it.year == state.selectedYear }?.seasons.orEmpty(),
                selectedSeason = state.selectedSeason,
                onSeasonSelected = { onEvent(SeasonArchiveEvent.OnSeasonSelected(it)) },
            )

            // Danh sách năm/mùa lỗi (offline lần đầu, chưa có cache) — báo lỗi
            // riêng vì đây là dữ liệu điều khiển chip, không phải nội dung grid.
            if (state.yearsError != null) {
                PagingRefreshErrorBanner(
                    message = state.yearsError,
                    onRetry = { onEvent(SeasonArchiveEvent.OnRetryYears) },
                )
            }

            // Banner lỗi nhẹ khi refresh (đổi năm/mùa) thất bại nhưng vẫn còn
            // item cũ để hiện — giữ cache cũ hiển thị + báo lỗi nhẹ, không che
            // grid bên dưới (jikan-api SKILL.md mục Caching, điểm 3).
            if (pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount > 0) {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                PagingRefreshErrorBanner(
                    message = error.toAppError().toUserMessage(),
                    onRetry = { pagingItems.retry() },
                )
            }

            when {
                // Không check itemCount == 0: khi đổi năm/mùa, flatMapLatest tạo Pager mới
                // nhưng LazyPagingItems vẫn giữ item cũ của lựa chọn trước cho tới khi
                // trang mới tải xong — ưu tiên refresh-loading để tránh hiện nhầm list cũ.
                pagingItems.loadState.refresh is LoadState.Loading -> PagingGridLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    PagingGridError(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> EmptyContent()

                else -> ResultsGrid(
                    pagingItems = pagingItems,
                    onAnimeClick = { onEvent(SeasonArchiveEvent.OnAnimeClick(it)) },
                )
            }
        }
    }
}

@Composable
private fun YearRow(years: List<SeasonYear>, selectedYear: Int?, onYearSelected: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(years, key = { it.year }) { year ->
            PickerChip(
                label = year.year.toString(),
                selected = year.year == selectedYear,
                onClick = { onYearSelected(year.year) },
            )
        }
    }
}

@Composable
private fun SeasonRow(availableSeasons: List<String>, selectedSeason: String?, onSeasonSelected: (String) -> Unit) {
    if (availableSeasons.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(seasonOrder.filter { it in availableSeasons }, key = { it }) { season ->
            PickerChip(
                label = seasonLabels[season] ?: season,
                selected = season == selectedSeason,
                onClick = { onSeasonSelected(season) },
            )
        }
    }
}

@Composable
private fun PickerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultsGrid(pagingItems: LazyPagingItems<Anime>, onAnimeClick: (Int) -> Unit) {
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

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có anime nào trong mùa này",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
