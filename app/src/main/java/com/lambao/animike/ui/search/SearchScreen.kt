package com.lambao.animike.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.Dimens

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
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
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun SearchScreenContent(
    state: SearchState,
    pagingItems: LazyPagingItems<Anime>,
    onBackClick: () -> Unit,
    onEvent: (SearchEvent) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
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
            }

            FilterRow(filters = state.filters, genres = state.genres, onEvent = onEvent)

            when {
                pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0 ->
                    LoadingContent()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    SearchErrorContent(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> SearchEmptyContent()

                else -> ResultsGrid(
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

@Composable
private fun FilterRow(filters: SearchFilters, genres: List<Genre>, onEvent: (SearchEvent) -> Unit) {
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            item(key = "type_all") {
                FilterChip(label = "Tất cả", selected = filters.type == null) {
                    onEvent(SearchEvent.OnTypeFilterChange(null))
                }
            }
            item(key = "type_tv") {
                FilterChip(label = "TV", selected = filters.type == "tv") {
                    onEvent(SearchEvent.OnTypeFilterChange("tv"))
                }
            }
            item(key = "type_movie") {
                FilterChip(label = "Movie", selected = filters.type == "movie") {
                    onEvent(SearchEvent.OnTypeFilterChange("movie"))
                }
            }
            item(key = "status_airing") {
                FilterChip(label = "Đang chiếu", selected = filters.status == "airing") {
                    onEvent(SearchEvent.OnStatusFilterChange(if (filters.status == "airing") null else "airing"))
                }
            }
            item(key = "sort_popular") {
                FilterChip(label = "Phổ biến", selected = filters.orderBy == "popularity") {
                    onEvent(SearchEvent.OnSortChange("popularity", "asc"))
                }
            }
            item(key = "sort_score") {
                FilterChip(label = "Điểm cao", selected = filters.orderBy == "score") {
                    onEvent(SearchEvent.OnSortChange("score", "desc"))
                }
            }
        }

        if (genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(genres, key = { it.id }) { genre ->
                    FilterChip(
                        label = genre.name,
                        selected = genre.id in filters.genreIds,
                        onClick = { onEvent(SearchEvent.OnGenreToggle(genre.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
        // của LazyGridScope (như item()/align()/weight()) — không cần import.
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
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SearchErrorContent(message: String, onRetry: () -> Unit) {
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
private fun SearchEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không tìm thấy kết quả",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
