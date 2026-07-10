package com.lambao.animike.ui.topcharacters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.TopCharacter
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.TopCharacterCard
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.Dimens

@Composable
fun TopCharactersScreen(
    onBackClick: () -> Unit,
    onNavigateToCharacterDetail: (Int) -> Unit,
    viewModel: TopCharactersViewModel = hiltViewModel(),
) {
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TopCharactersEffect.NavigateToCharacterDetail -> onNavigateToCharacterDetail(effect.characterId)
            }
        }
    }

    TopCharactersScreenContent(pagingItems = pagingItems, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun TopCharactersScreenContent(
    pagingItems: LazyPagingItems<TopCharacter>,
    onBackClick: () -> Unit,
    onEvent: (TopCharactersEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ insets.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Top nhân vật",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0 ->
                    TopCharactersGridLoading()

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    TopCharactersError(message = error.toAppError().toUserMessage(), onRetry = { pagingItems.retry() })
                }

                pagingItems.itemCount == 0 -> TopCharactersEmpty()

                else -> TopCharactersGrid(
                    pagingItems = pagingItems,
                    onCharacterClick = { onEvent(TopCharactersEvent.OnCharacterClick(it)) },
                )
            }
        }
    }
}

// Lưới 2 cột (KHÁC 3 cột của AnimePagingGrid) — ảnh nhân vật dọc hơn poster
// anime nên 2 cột cho card to, dễ thấy huy hiệu hạng + tên. rank = index+1
// (list sort favorites giảm dần, dedup giữ thứ tự) — ribbon chỉ hiện top-3.
@Composable
private fun TopCharactersGrid(pagingItems: LazyPagingItems<TopCharacter>, onCharacterClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.malId },
            contentType = pagingItems.itemContentType { "topCharacter" },
        ) { index ->
            pagingItems[index]?.let { character ->
                TopCharacterCard(
                    character = character,
                    onClick = { onCharacterClick(character.malId) },
                    modifier = Modifier.fillMaxWidth(),
                    rank = index + 1,
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val shimmerProgress = rememberShimmerProgress()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    repeat(2) {
                        AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceMd), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { pagingItems.retry() }) {
                        Text("Tải thêm thất bại — Thử lại")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCharactersGridLoading() {
    val shimmerProgress = rememberShimmerProgress()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(6, key = { it }) {
            AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TopCharactersError(message: String, onRetry: () -> Unit) {
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
private fun TopCharactersEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Chưa có nhân vật nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
