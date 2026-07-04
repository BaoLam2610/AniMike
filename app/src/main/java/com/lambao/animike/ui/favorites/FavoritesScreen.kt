package com.lambao.animike.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.theme.Dimens

@Composable
fun FavoritesScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FavoritesEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    FavoritesScreenContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun FavoritesScreenContent(state: FavoritesState, onEvent: (FavoritesEvent) -> Unit) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets cho bottom nav — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = "Yêu thích",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )

            if (state.favorites.isEmpty()) {
                EmptyFavoritesContent()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    items(state.favorites, key = { it.malId }) { anime ->
                        AnimeCard(
                            anime = anime,
                            onClick = { onEvent(FavoritesEvent.OnAnimeClick(anime.malId)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(text = "♡", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Chưa có anime yêu thích nào",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
