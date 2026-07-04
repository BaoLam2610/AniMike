package com.lambao.animike.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimePagingGrid
import com.lambao.animike.ui.components.PagingGridError
import com.lambao.animike.ui.components.PagingGridLoading
import com.lambao.animike.ui.components.PagingRefreshErrorBanner
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
    pagingItems: LazyPagingItems<Anime>,
    onEvent: (SchedulesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DayRow(
            selectedDay = state.selectedDay,
            onDaySelected = { onEvent(SchedulesEvent.OnDaySelected(it)) },
        )

        // Banner lỗi nhẹ khi refresh (đổi thứ) thất bại nhưng vẫn còn item
        // cũ để hiện — giữ cache cũ hiển thị + báo lỗi nhẹ, không che grid
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
            pagingItems.loadState.refresh is LoadState.Loading -> PagingGridLoading()

            pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                PagingGridError(
                    message = error.toAppError().toUserMessage(),
                    onRetry = { pagingItems.retry() },
                )
            }

            pagingItems.itemCount == 0 -> EmptyContent()

            else -> AnimePagingGrid(
                pagingItems = pagingItems,
                onAnimeClick = { onEvent(SchedulesEvent.OnAnimeClick(it)) },
            )
        }
    }
}

@Composable
private fun DayRow(selectedDay: String, onDaySelected: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(weekDays, key = { it }) { day ->
            PickerChip(
                label = weekDayLabels[day] ?: day,
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
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
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có anime nào chiếu vào ngày này",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
