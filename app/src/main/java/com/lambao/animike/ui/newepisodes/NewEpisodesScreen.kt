package com.lambao.animike.ui.newepisodes

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.domain.model.NewEpisodeRelease
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.NewEpisodeCard
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.Dimens

@Composable
fun NewEpisodesScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: NewEpisodesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NewEpisodesEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    NewEpisodesScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun NewEpisodesScreenContent(
    state: NewEpisodesState,
    onBackClick: () -> Unit,
    onEvent: (NewEpisodesEvent) -> Unit,
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
                    text = "Tập mới phát hành",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                // Cache-first: còn dữ liệu (từ Room, đọc chung Flow với Home)
                // thì luôn hiện, kể cả khi đang refresh nền hoặc refresh vừa lỗi.
                state.releases.isNotEmpty() -> NewEpisodesGrid(
                    releases = state.releases,
                    onAnimeClick = { onEvent(NewEpisodesEvent.OnAnimeClick(it)) },
                )

                state.isLoading -> NewEpisodesGridLoading()

                state.error != null -> NewEpisodesErrorContent(
                    message = state.error,
                    onRetry = { onEvent(NewEpisodesEvent.OnRetry) },
                )

                else -> NewEpisodesEmptyContent()
            }
        }
    }
}

@Composable
private fun NewEpisodesGrid(
    releases: List<NewEpisodeRelease>,
    onAnimeClick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(releases, key = { it.malId }) { release ->
            NewEpisodeCard(
                release = release,
                onClick = { onAnimeClick(release.malId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NewEpisodesGridLoading() {
    val shimmerProgress = rememberShimmerProgress()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(count = 8, key = { it }) {
            AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NewEpisodesErrorContent(message: String, onRetry: () -> Unit) {
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
private fun NewEpisodesEmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Chưa có tập mới nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
