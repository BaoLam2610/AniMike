package com.lambao.animike.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.RelationGroup
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.success
import androidx.core.net.toUri

@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailEffect.OpenYoutube -> {
                    val uri = "https://www.youtube.com/watch?v=${effect.videoId}".toUri()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: ActivityNotFoundException) {
                        Log.w("DetailScreen", "Không có app nào xử lý được link YouTube", e)
                    }
                }

                is DetailEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    DetailScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun DetailScreenContent(
    state: DetailState,
    onBackClick: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isLoading -> LoadingContent(onBackClick)
                state.error != null -> ErrorContent(
                    message = state.error,
                    onBackClick = onBackClick,
                    onRetry = { onEvent(DetailEvent.OnRetry) },
                )

                state.detail != null -> DetailContent(
                    detail = state.detail,
                    characters = state.characters,
                    recommendations = state.recommendations,
                    onBackClick = onBackClick,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: AnimeDetail,
    characters: List<AnimeCharacter>,
    recommendations: List<Anime>,
    onBackClick: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeroHeader(detail = detail, onBackClick = onBackClick) }

        if (detail.genres.isNotEmpty()) {
            item { GenreChips(genres = detail.genres) }
        }

        if (detail.trailerYoutubeId != null) {
            item {
                Button(
                    onClick = { onEvent(DetailEvent.OnTrailerClick) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                ) {
                    Text("Xem trailer")
                }
            }
        }

        item { SynopsisSection(synopsis = detail.synopsis) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { CharactersSection(characters = characters) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { RelationsSection(relations = detail.relations) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            RecommendationsSection(
                recommendations = recommendations,
                onClick = { onEvent(DetailEvent.OnRecommendationClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceXl)) }
    }
}

@Composable
private fun HeroHeader(detail: AnimeDetail, onBackClick: () -> Unit) {
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxWidth().height(Dimens.HeroHeaderHeight)) {
        AsyncImage(
            model = detail.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient overlay #0B0E14 alpha 0->85% bottom-up (animike-design SKILL.md)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)),
                    ),
                ),
        )
        BackButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(Dimens.SpaceSm))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                ScoreBadge(score = detail.score)
                if (detail.isAiring) AiringPill()
            }
            Text(
                text = detail.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = detailMetaLine(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun detailMetaLine(detail: AnimeDetail): String = listOfNotNull(
    detail.type,
    detail.year?.toString(),
    detail.episodes?.let { "$it tập" },
    detail.studios.takeIf { it != "N/A" },
).joinToString(" · ")

@Composable
private fun ScoreBadge(score: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "★ $score",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AiringPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "ĐANG CHIẾU",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.success,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.IconButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Quay lại" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "←",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun GenreChips(genres: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(genres, key = { it }) { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SynopsisSection(synopsis: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
        Text(
            text = "Nội dung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = synopsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = if (expanded) "Thu gọn" else "Xem thêm",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = Dimens.SpaceXs)
                .clickable { expanded = !expanded },
        )
    }
}

@Composable
private fun CharactersSection(characters: List<AnimeCharacter>) {
    if (characters.isEmpty()) return
    Column {
        Text(
            text = "Nhân vật & Seiyuu",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            items(characters, key = { it.malId }) { character -> CharacterItem(character) }
        }
    }
}

@Composable
private fun CharacterItem(character: AnimeCharacter) {
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Dimens.AvatarSize),
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.size(Dimens.AvatarSize).clip(CircleShape),
        )
        Text(
            text = character.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        character.voiceActorName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RelationsSection(relations: List<RelationGroup>) {
    if (relations.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = "Liên quan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        relations.forEach { group ->
            Text(
                text = "${group.relation}: ${group.titles.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendationsSection(recommendations: List<Anime>, onClick: (Int) -> Unit) {
    if (recommendations.isEmpty()) return
    Column {
        Text(
            text = "Đề xuất tương tự",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            items(recommendations, key = { it.malId }) { anime ->
                AnimeCard(anime = anime, onClick = { onClick(anime.malId) })
            }
        }
    }
}

@Composable
private fun LoadingContent(onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        BackButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(Dimens.SpaceSm))
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onBackClick: () -> Unit, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        BackButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(Dimens.SpaceSm))
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Dimens.ScreenPadding),
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
