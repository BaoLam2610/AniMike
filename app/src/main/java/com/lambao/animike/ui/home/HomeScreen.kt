package com.lambao.animike.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.AniMikeTheme
import com.lambao.animike.ui.theme.Dimens

@Composable
fun HomeScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    HomeScreenContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun HomeScreenContent(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXl),
        ) {
            item {
                AnimeSection(
                    title = "Season Now",
                    section = state.seasonNow,
                    onRetry = { onEvent(HomeEvent.OnRetrySeasonNow) },
                    onAnimeClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                )
            }
            item {
                AnimeSection(
                    title = "Top Anime",
                    section = state.topAnime,
                    onRetry = { onEvent(HomeEvent.OnRetryTopAnime) },
                    onAnimeClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                )
            }
            item {
                AnimeSection(
                    title = "Sắp chiếu",
                    section = state.upcoming,
                    onRetry = { onEvent(HomeEvent.OnRetryUpcoming) },
                    onAnimeClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                )
            }
        }
    }
}

@Composable
private fun AnimeSection(
    title: String,
    section: SectionState,
    onRetry: () -> Unit,
    onAnimeClick: (Int) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        when {
            section.isLoading -> {
                // Hoist 1 animation dùng chung cho cả 6 placeholder của section này
                val shimmerProgress = rememberShimmerProgress()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    items(6, key = { it }) { AnimeCardPlaceholder(progress = shimmerProgress) }
                }
            }

            section.error != null -> SectionError(message = section.error, onRetry = onRetry)

            section.animeList.isEmpty() -> Text(
                text = "Chưa có dữ liệu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            )

            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                items(section.animeList, key = { it.malId }) { anime ->
                    AnimeCard(anime = anime, onClick = { onAnimeClick(anime.malId) })
                }
            }
        }
    }
}

@Composable
private fun SectionError(message: String, onRetry: () -> Unit) {
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

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun HomeScreenPreview() {
    AniMikeTheme {
        HomeScreenContent(
            state = HomeState(
                seasonNow = SectionState(
                    isLoading = false,
                    animeList = listOf(
                        Anime(1, "Sousou no Frieren", null, "9.1", 2023),
                        Anime(2, "Ao no Hako", null, "8.4", 2024),
                    ),
                ),
                topAnime = SectionState(
                    isLoading = false,
                    animeList = listOf(
                        Anime(3, "Fullmetal Alchemist: Brotherhood", null, "9.3", 2009),
                        Anime(4, "Steins;Gate", null, "9.2", 2011),
                    ),
                ),
                upcoming = SectionState(isLoading = true),
            ),
            onEvent = {},
        )
    }
}
