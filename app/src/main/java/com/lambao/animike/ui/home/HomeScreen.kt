package com.lambao.animike.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.theme.AniMikeTheme
import com.lambao.animike.ui.theme.Dimens

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreenContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun HomeScreenContent(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                // Spinner giữa màn hình chỉ dùng cho lần tải đầu (animike-design SKILL.md)
                state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(HomeEvent.OnRetry) },
                )

                state.animeList.isEmpty() -> EmptyContent()

                else -> AnimeList(animeList = state.animeList)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
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

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(text = "(・_・)", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Chưa có anime nào để hiển thị",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnimeList(animeList: List<Anime>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(animeList, key = { it.malId }) { anime ->
            AnimeRow(anime)
        }
    }
}

@Composable
private fun AnimeRow(anime: Anime) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.RadiusCard))
            .padding(Dimens.SpaceMd),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = anime.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.SpaceSm),
            maxLines = 2,
        )
        Text(
            text = "★ ${anime.score}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun HomeScreenPreview() {
    AniMikeTheme {
        HomeScreenContent(
            state = HomeState(
                isLoading = false,
                animeList = listOf(
                    Anime(1, "Fullmetal Alchemist: Brotherhood", null, "9.3", 2009),
                    Anime(2, "Steins;Gate", null, "9.2", 2011),
                ),
            ),
            onEvent = {},
        )
    }
}
