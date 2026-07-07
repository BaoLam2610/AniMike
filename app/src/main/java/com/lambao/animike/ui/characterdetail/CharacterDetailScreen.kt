package com.lambao.animike.ui.characterdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.CharacterAnimeAppearance
import com.lambao.animike.domain.model.CharacterDetail
import com.lambao.animike.domain.model.CharacterVoiceActor
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.ExpandableText
import com.lambao.animike.ui.theme.Dimens

@Composable
fun CharacterDetailScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPersonDetail: (Int) -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CharacterDetailEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
                is CharacterDetailEffect.NavigateToPersonDetail -> onNavigateToPersonDetail(effect.personMalId)
            }
        }
    }

    CharacterDetailScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun CharacterDetailScreenContent(
    state: CharacterDetailState,
    onBackClick: () -> Unit,
    onEvent: (CharacterDetailEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                // Cache-first: đã có character (từ Room) thì luôn hiện, kể cả
                // khi đang refresh nền hoặc refresh vừa lỗi (stale-while-revalidate).
                state.character != null -> CharacterDetailContent(
                    character = state.character,
                    animeAppearances = state.animeAppearances,
                    voiceActors = state.voiceActors,
                    onEvent = onEvent,
                )

                state.isLoading -> LoadingContent()

                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(CharacterDetailEvent.OnRetry) },
                )

                else -> LoadingContent()
            }

            // Neo cố định back (KHÔNG đặt trong CharacterHero) — sibling của
            // LazyColumn trong Box này, luôn nổi trên cùng bất kể cuộn tới đâu
            // hay đang ở state nào (cùng lý do DetailScreen.TopBar).
            BackButton(onClick = onBackClick, modifier = Modifier.padding(Dimens.SpaceSm))
        }
    }
}

@Composable
private fun CharacterDetailContent(
    character: CharacterDetail,
    animeAppearances: List<CharacterAnimeAppearance>,
    voiceActors: List<CharacterVoiceActor>,
    onEvent: (CharacterDetailEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { CharacterHero(character = character) }

        if (character.nicknames.isNotEmpty()) {
            item { NicknameChips(nicknames = character.nicknames) }
        }

        if (!character.about.isNullOrBlank()) {
            item {
                Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm)) {
                    Text(
                        text = "Giới thiệu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    ExpandableText(
                        text = character.about,
                        modifier = Modifier.padding(top = Dimens.SpaceXs),
                        maxCollapsedLines = 4,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            AnimeAppearancesSection(
                appearances = animeAppearances,
                onAnimeClick = { onEvent(CharacterDetailEvent.OnAnimeClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            VoiceActorsSection(
                voiceActors = voiceActors,
                onVoiceActorClick = { onEvent(CharacterDetailEvent.OnVoiceActorClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceXl)) }
    }
}

// Hero portrait (KHÁC HeroHeader 16:9 của AnimeDetail ở DetailScreen) — ảnh
// nhân vật thường dọc hơn cover anime, nhưng vẫn dùng chung chiều cao cố định
// HeroHeaderHeight (Crop tự xử lý mọi tỉ lệ ảnh gốc) để nhất quán với Detail.
@Composable
private fun CharacterHero(character: CharacterDetail) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.HeroHeaderHeight),
    ) {
        AsyncImage(
            model = character.imageUrl,
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
                    Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f))),
                ),
        )
        // favorites=0 nghĩa là API không trả (mapper mặc định 0) — ẩn badge
        // thay vì hiện "♥ 0", cùng quy tắc ẩn "N/A" của ScoreBadge.
        if (character.favorites > 0) {
            FavoritesBadge(
                favorites = character.favorites,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.ScreenPadding),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = character.name,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (character.nameKanji != null) {
                Text(
                    text = character.nameKanji,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Badge nền surface bán trong suốt đè trên ảnh (cùng style HeroScoreBadge ở
// DetailScreen) — accent secondary (hồng) vì đây là biểu tượng "yêu thích",
// khớp quy ước FavoriteButton (♥ active = secondary) của animike-design.
@Composable
private fun FavoritesBadge(favorites: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "♥ $favorites",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun NicknameChips(nicknames: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(nicknames, key = { it }) { nickname ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
            ) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// AnimatedVisibility thay vì early return — anime[] fetch xong CÙNG LÚC với
// core (1 API call /characters/{id}/full) nhưng qua Flow riêng (xem
// CharacterDetailRepository), có thể re-emit ở compose pass sau — cùng lý do
// CharactersSection/PicturesSection ở DetailScreen.
@Composable
private fun AnimeAppearancesSection(appearances: List<CharacterAnimeAppearance>, onAnimeClick: (Int) -> Unit) {
    AnimatedVisibility(visible = appearances.isNotEmpty()) {
        Column {
            Text(
                text = "Xuất hiện trong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                items(appearances, key = { it.anime.malId }) { appearance ->
                    AnimeAppearanceItem(appearance = appearance, onClick = { onAnimeClick(appearance.anime.malId) })
                }
            }
        }
    }
}

@Composable
private fun AnimeAppearanceItem(appearance: CharacterAnimeAppearance, onClick: () -> Unit) {
    Column(modifier = Modifier.width(Dimens.CardWidth)) {
        AnimeCard(anime = appearance.anime, onClick = onClick, modifier = Modifier.fillMaxWidth())
        if (appearance.role.isNotEmpty()) {
            Text(
                text = appearance.role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.SpaceXs),
            )
        }
    }
}

// Cùng lý do AnimatedVisibility với AnimeAppearancesSection ở trên.
@Composable
private fun VoiceActorsSection(voiceActors: List<CharacterVoiceActor>, onVoiceActorClick: (Int) -> Unit) {
    AnimatedVisibility(visible = voiceActors.isNotEmpty()) {
        Column {
            Text(
                text = "Lồng tiếng bởi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                items(voiceActors, key = { it.personMalId }) { voiceActor ->
                    VoiceActorItem(voiceActor, onClick = { onVoiceActorClick(voiceActor.personMalId) })
                }
            }
        }
    }
}

// MVP5 mục 2 (People Detail) đã có — bấm mở People Detail.
@Composable
private fun VoiceActorItem(voiceActor: CharacterVoiceActor, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(Dimens.AvatarSize)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = voiceActor.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .size(Dimens.AvatarSize)
                .clip(CircleShape),
        )
        Text(
            text = voiceActor.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = voiceActor.language,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
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
