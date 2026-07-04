package com.lambao.animike.ui.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens

@Composable
fun CharactersScreen(
    onBackClick: () -> Unit,
    viewModel: CharactersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CharactersScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun CharactersScreenContent(
    state: CharactersState,
    onBackClick: () -> Unit,
    onEvent: (CharactersEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                SearchField(
                    query = state.query,
                    onQueryChange = { onEvent(CharactersEvent.OnQueryChange(it)) },
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                // Cache-first tinh thần chung: còn dữ liệu thì luôn hiện — màn
                // này không cache Room (dữ liệu phụ, xem AnimeDetailRepository).
                state.allCharacters.isNotEmpty() -> CharactersGrid(characters = state.filteredCharacters)
                state.isLoading -> CharactersGridLoading()
                state.error != null -> CharactersErrorContent(
                    message = state.error,
                    onRetry = { onEvent(CharactersEvent.OnRetry) },
                )
                else -> CharactersEmptyContent()
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(text = "Tìm nhân vật...", style = MaterialTheme.typography.bodyMedium)
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
private fun CharactersGrid(characters: List<AnimeCharacter>) {
    if (characters.isEmpty()) {
        CharactersEmptyContent(message = "Không tìm thấy nhân vật")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(characters, key = { it.malId }) { character -> CharacterGridItem(character) }
    }
}

@Composable
private fun CharacterGridItem(character: AnimeCharacter) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape),
        )
        Text(
            text = character.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = character.voiceActorName ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CharactersGridLoading() {
    val shimmerProgress = rememberShimmerProgress()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(count = 12, key = { it }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .shimmerEffect(shimmerProgress),
            )
        }
    }
}

@Composable
private fun CharactersErrorContent(message: String, onRetry: () -> Unit) {
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
private fun CharactersEmptyContent(message: String = "Không có nhân vật nào") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
