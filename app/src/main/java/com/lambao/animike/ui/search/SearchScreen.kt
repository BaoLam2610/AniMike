package com.lambao.animike.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimePagingGrid
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.PagingGridError
import com.lambao.animike.ui.components.PagingGridLoading
import com.lambao.animike.ui.components.PagingRefreshErrorBanner
import com.lambao.animike.ui.theme.Dimens

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToFilter: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    SearchScreenContent(
        state = state,
        pagingItems = pagingItems,
        onBackClick = onBackClick,
        onNavigateToFilter = onNavigateToFilter,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun SearchScreenContent(
    state: SearchState,
    pagingItems: LazyPagingItems<Anime>,
    onBackClick: () -> Unit,
    onNavigateToFilter: () -> Unit,
    onEvent: (SearchEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                SearchTextField(
                    query = state.query,
                    onQueryChange = { onEvent(SearchEvent.OnQueryChange(it)) },
                    modifier = Modifier.weight(1f),
                )
                // Thay 2 hàng chip cũ (type/status/sort/genre) — mở màn full-screen
                // "Sắp xếp & Lọc" riêng (kit Animax MVP3 UI-6, Reset/Apply).
                FilterButton(onClick = onNavigateToFilter)
            }

            ActiveFiltersRow(
                filters = state.filters,
                genres = state.genres,
                onChipClick = onNavigateToFilter,
            )

            // Banner lỗi nhẹ khi refresh (đổi query/filter) thất bại nhưng vẫn còn
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
                // Không check itemCount == 0: đổi query/filter tạo PagingSource mới nhưng
                // LazyPagingItems vẫn giữ item cũ cho tới khi trang mới tải xong — ưu tiên
                // refresh-loading để tránh hiện nhầm kết quả của query/filter trước.
                pagingItems.loadState.refresh is LoadState.Loading -> PagingGridLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    PagingGridError(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> SearchEmptyContent()

                else -> AnimePagingGrid(
                    pagingItems = pagingItems,
                    onAnimeClick = { onEvent(SearchEvent.OnAnimeClick(it)) },
                )
            }
        }
    }
}

@Composable
private fun SearchTextField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(text = "Tìm kiếm anime...", style = MaterialTheme.typography.bodyMedium)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .semantics { contentDescription = "Xóa tìm kiếm" }
                        .padding(Dimens.SpaceMd),
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusButton),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

// Hàng chip tóm tắt filter đang áp dụng, ngay dưới thanh search (kit Animax
// 29_Dark_sort & filter results) — chỉ hiện khi khác mặc định để khỏi chiếm
// chỗ vô ích; bấm chip nào cũng mở lại màn "Sắp xếp & Lọc" để chỉnh.
@Composable
private fun ActiveFiltersRow(
    filters: SearchFilters,
    genres: List<Genre>,
    onChipClick: () -> Unit,
) {
    // remember theo (filters, genres): danh sách nhãn chỉ đổi khi Apply filter
    // mới hoặc cache genres load xong — không rebuild mỗi lần gõ query.
    val labels = remember(filters, genres) {
        buildList {
            when (filters.orderBy) {
                "score" -> add("Điểm cao")
                "start_date" -> add("Mới nhất")
                // "popularity" là mặc định — không hiện chip cho đỡ rối.
            }
            when (filters.type) {
                "tv" -> add("TV")
                "movie" -> add("Movie")
            }
            // Genre id không có trong cache (chưa load xong) thì bỏ qua nhãn đó
            // thay vì hiện id thô — cache load xong sẽ tự recompose đủ nhãn.
            filters.genreIds.forEach { id ->
                genres.firstOrNull { it.id == id }?.let { add(it.name) }
            }
            filters.year?.let { add("$it") }
        }
    }
    if (labels.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(labels, key = { it }) { label ->
            SelectableChip(label = label, selected = true, onClick = onChipClick)
        }
    }
}

@Composable
private fun FilterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.IconButtonSize)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Sắp xếp & Lọc" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🎚", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SearchEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không tìm thấy kết quả",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
