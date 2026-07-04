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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.Episode
import com.lambao.animike.domain.model.RelationGroup
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.BackButton
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
                // Cache-first: đã có detail (từ Room) thì luôn hiện, kể cả khi
                // đang refresh nền hoặc refresh vừa lỗi (stale-while-revalidate).
                state.detail != null -> DetailContent(
                    detail = state.detail,
                    characters = state.characters,
                    recommendations = state.recommendations,
                    episodes = state.episodes,
                    reviews = state.reviews,
                    isFavorite = state.isFavorite,
                    onBackClick = onBackClick,
                    onEvent = onEvent,
                )

                state.isLoading -> LoadingContent(onBackClick)

                state.error != null -> ErrorContent(
                    message = state.error,
                    onBackClick = onBackClick,
                    onRetry = { onEvent(DetailEvent.OnRetry) },
                )

                // Lưới an toàn: khoảng hở ngắn giữa lúc refresh xong (isLoading=false)
                // và lúc Flow từ Room re-emit detail — không để màn trắng thoáng qua.
                else -> LoadingContent(onBackClick)
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: AnimeDetail,
    characters: List<AnimeCharacter>,
    recommendations: List<Anime>,
    episodes: List<Episode>,
    reviews: List<AnimeReview>,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeroHeader(
                detail = detail,
                isFavorite = isFavorite,
                onBackClick = onBackClick,
                onFavoriteClick = { onEvent(DetailEvent.OnFavoriteClick) },
            )
        }

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
        item { EpisodesSection(episodes = episodes) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { CharactersSection(characters = characters) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { RelationsSection(relations = detail.relations) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            RecommendationsOrReviewsSection(
                recommendations = recommendations,
                reviews = reviews,
                onRecommendationClick = { onEvent(DetailEvent.OnRecommendationClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceXl)) }
    }
}

@Composable
private fun HeroHeader(
    detail: AnimeDetail,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    // ColorPainter không override equals() — remember để tránh AsyncImage coi
    // placeholder/error/fallback là "đổi" ở mỗi recomposition.
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
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
        FavoriteButton(
            isFavorite = isFavorite,
            onClick = onFavoriteClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceSm),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                if (detail.score != "N/A") HeroScoreBadge(score = detail.score)
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

// Style riêng cho hero header (nền surface alpha 80% đè trên ảnh cover, không
// phải nền primary như ScoreBadge trên AnimeCard — 2 ngữ cảnh khác nhau, xem
// animike-design SKILL.md mục Score badge).
@Composable
private fun HeroScoreBadge(score: String) {
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
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.IconButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.titleMedium,
            color = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
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
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
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

// Không có thumbnail per-episode từ Jikan (/anime/{id}/episodes chỉ trả
// title/aired/filler/recap) — chỉ page 1 (100 tập), đủ cho đa số anime.
@Composable
private fun EpisodesSection(episodes: List<Episode>) {
    if (episodes.isEmpty()) return
    Column {
        Text(
            text = "Các tập",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            items(episodes, key = { it.number }) { episode -> EpisodeItem(episode) }
        }
    }
}

@Composable
private fun EpisodeItem(episode: Episode) {
    Column(
        modifier = Modifier
            .width(Dimens.EpisodeCardWidth)
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceMd),
    ) {
        Text(
            text = "Tập ${episode.number}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        if (episode.isFiller || episode.isRecap) {
            Text(
                text = if (episode.isFiller) "Filler" else "Recap",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpaceXs),
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

// Kit Animax: tab "More Like This" / "Comments" (underline indicator). Ở đây
// đổi tên "Comments" thành "Đánh giá" vì Jikan chỉ có reviews MAL, không có
// bình luận thời gian thực. Chỉ hiện tab khi CẢ 2 đều có dữ liệu — nếu chỉ 1
// bên có, hiện thẳng section đó (không cần tab để chọn 1 lựa chọn duy nhất).
private enum class DetailTab { RECOMMENDATIONS, REVIEWS }

@Composable
private fun RecommendationsOrReviewsSection(
    recommendations: List<Anime>,
    reviews: List<AnimeReview>,
    onRecommendationClick: (Int) -> Unit,
) {
    when {
        recommendations.isEmpty() && reviews.isEmpty() -> return

        recommendations.isNotEmpty() && reviews.isNotEmpty() -> {
            var selectedTab by rememberSaveable { mutableStateOf(DetailTab.RECOMMENDATIONS) }
            Column {
                DetailTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                when (selectedTab) {
                    DetailTab.RECOMMENDATIONS -> RecommendationsRow(recommendations, onRecommendationClick)
                    DetailTab.REVIEWS -> ReviewsList(reviews)
                }
            }
        }

        recommendations.isNotEmpty() -> Column {
            SectionTitle("Đề xuất tương tự")
            RecommendationsRow(recommendations, onRecommendationClick)
        }

        else -> Column {
            SectionTitle("Đánh giá")
            ReviewsList(reviews)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
    )
}

@Composable
private fun DetailTabRow(selectedTab: DetailTab, onTabSelected: (DetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        DetailTabItem(
            label = "Đề xuất",
            selected = selectedTab == DetailTab.RECOMMENDATIONS,
            onClick = { onTabSelected(DetailTab.RECOMMENDATIONS) },
        )
        DetailTabItem(
            label = "Đánh giá",
            selected = selectedTab == DetailTab.REVIEWS,
            onClick = { onTabSelected(DetailTab.REVIEWS) },
        )
    }
}

// width(IntrinsicSize.Min) bắt buộc Column đo intrinsic width của Text trước
// (thay vì chỉ nhận constraint "phần còn lại của Row" từ trên xuống) — nếu bỏ
// dòng này, Box.fillMaxWidth() bên dưới sẽ fill theo constraint đó (gần hết
// Row) thay vì theo đúng độ rộng chữ, và item đầu tiên sẽ chiếm hết chỗ của
// item thứ 2 (Row mặc định không đo theo sibling, xem Modifier.width doc).
@Composable
private fun DetailTabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TabIndicatorHeight)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun RecommendationsRow(recommendations: List<Anime>, onClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(recommendations, key = { it.malId }) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onClick(anime.malId) },
                modifier = Modifier.width(Dimens.CardWidth),
            )
        }
    }
}

// reviews đã được giới hạn số lượng ở AnimeDetailRepository.getReviews() —
// chưa cần phân trang/expand cho MVP.
@Composable
private fun ReviewsList(reviews: List<AnimeReview>) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        reviews.forEach { review -> ReviewCard(review) }
    }
}

@Composable
private fun ReviewCard(review: AnimeReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = review.username,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (review.score != null) {
                Text(
                    text = "★ ${review.score}/10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(
            text = review.reviewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
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
