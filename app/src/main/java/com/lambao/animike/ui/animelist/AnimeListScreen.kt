package com.lambao.animike.ui.animelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeListSource
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimePagingGrid
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.PagingGridError
import com.lambao.animike.ui.components.PagingGridLoading
import com.lambao.animike.ui.theme.Dimens

@Composable
fun AnimeListScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AnimeListViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AnimeListEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    AnimeListScreenContent(
        title = when (viewModel.source) {
            AnimeListSource.TOP -> "Top Hits Anime"
            AnimeListSource.UPCOMING -> "Sắp chiếu"
        },
        pagingItems = pagingItems,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun AnimeListScreenContent(
    title: String,
    pagingItems: LazyPagingItems<Anime>,
    onBackClick: () -> Unit,
    onEvent: (AnimeListEvent) -> Unit,
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                // Không check itemCount == 0: giống Episodes/Reviews — ưu tiên
                // refresh-loading, source cố định nên không tạo lại Pager giữa chừng.
                pagingItems.loadState.refresh is LoadState.Loading -> PagingGridLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    PagingGridError(
                        message = error.toAppError().toUserMessage(),
                        onRetry = { pagingItems.retry() },
                    )
                }

                pagingItems.itemCount == 0 -> AnimeListEmptyContent()

                else -> AnimePagingGrid(
                    pagingItems = pagingItems,
                    onAnimeClick = { onEvent(AnimeListEvent.OnAnimeClick(it)) },
                )
            }
        }
    }
}

@Composable
private fun AnimeListEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Chưa có dữ liệu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
