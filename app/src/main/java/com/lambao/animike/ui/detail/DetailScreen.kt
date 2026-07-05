package com.lambao.animike.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onNavigateToEpisodes: (Int) -> Unit,
    onNavigateToCharacters: (Int) -> Unit,
    onNavigateToReviews: (Int) -> Unit,
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
                is DetailEffect.NavigateToEpisodes -> onNavigateToEpisodes(effect.malId)
                is DetailEffect.NavigateToCharacters -> onNavigateToCharacters(effect.malId)
                is DetailEffect.NavigateToReviews -> onNavigateToReviews(effect.malId)
            }
        }
    }

    DetailScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
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
                // Pull-to-refresh (docs/ROADMAP.md mục 3b) — force refresh
                // detail + recommendations/reviews/pictures bất kể TTL. Episodes
                // không cần force vì vốn đã luôn gọi lại API ở mọi lần loadAll().
                state.detail != null -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(DetailEvent.OnPullToRefresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DetailContent(
                        detail = state.detail,
                        characters = state.characters,
                        recommendations = state.recommendations,
                        episodes = state.episodes,
                        reviews = state.reviews,
                        pictures = state.pictures,
                        onEvent = onEvent,
                    )
                }

                state.isLoading -> LoadingContent()

                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(DetailEvent.OnRetry) },
                )

                // Lưới an toàn: khoảng hở ngắn giữa lúc refresh xong (isLoading=false)
                // và lúc Flow từ Room re-emit detail — không để màn trắng thoáng qua.
                else -> LoadingContent()
            }

            // Neo cố định back/favorite trên cùng, KHÔNG đặt trong HeroHeader
            // (trước đây nằm trong LazyColumn's item nên cuộn mất theo nội
            // dung) — đặt ở đây, là sibling của LazyColumn trong Box này, nên
            // luôn nổi trên cùng bất kể cuộn tới đâu hay đang ở state nào.
            TopBar(
                isFavorite = state.isFavorite,
                showFavorite = state.detail != null,
                onBackClick = onBackClick,
                onFavoriteClick = { onEvent(DetailEvent.OnFavoriteClick) },
            )
        }
    }
}

@Composable
private fun TopBar(isFavorite: Boolean, showFavorite: Boolean, onBackClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BackButton(onClick = onBackClick)
        // Luôn render đủ 2 slot (Spacer cùng kích thước khi chưa có favorite)
        // để Back không bị lệch vị trí phụ thuộc vào Arrangement.SpaceBetween
        // xử lý trường hợp 1-vs-2 con khác nhau.
        if (showFavorite) {
            FavoriteButton(isFavorite = isFavorite, onClick = onFavoriteClick)
        } else {
            Spacer(Modifier.size(Dimens.IconButtonSize))
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
    pictures: List<String>,
    onEvent: (DetailEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeroHeader(detail = detail) }

        if (detail.genres.isNotEmpty()) {
            item { GenreChips(genres = detail.genres) }
        }

        val trailerYoutubeId = detail.trailerYoutubeId
        if (trailerYoutubeId != null) {
            item {
                TrailerCard(
                    thumbnailUrl = detail.trailerThumbnailUrl,
                    onClick = { onEvent(DetailEvent.OnTrailerClick(trailerYoutubeId)) },
                )
            }
        }

        item { SynopsisSection(synopsis = detail.synopsis) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            EpisodesSection(
                episodes = episodes,
                onSeeAllClick = { onEvent(DetailEvent.OnSeeAllEpisodesClick) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            CharactersSection(
                characters = characters,
                onSeeAllClick = { onEvent(DetailEvent.OnSeeAllCharactersClick) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { RelationsSection(relations = detail.relations) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        // "Hình ảnh" đặt sau nhóm thông tin về chính bộ phim (tập/nhân vật/
        // liên quan), trước nhóm khám phá (Đề xuất/Đánh giá) — tính năng phát
        // sinh ngoài kit Animax, xem docs/ROADMAP.md MVP3.
        item { PicturesSection(pictures = pictures) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            RecommendationsOrReviewsSection(
                recommendations = recommendations,
                reviews = reviews,
                onRecommendationClick = { onEvent(DetailEvent.OnRecommendationClick(it)) },
                onSeeAllReviewsClick = { onEvent(DetailEvent.OnSeeAllReviewsClick) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceXl)) }
    }
}

@Composable
private fun HeroHeader(detail: AnimeDetail) {
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
        // Back/Favorite giờ neo cố định ở DetailScreenContent.TopBar (nổi trên
        // toàn màn hình, không cuộn theo), không render lại ở đây nữa.

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

// Thay nút text "Xem trailer" cũ bằng card thumbnail 16:9 + play overlay —
// vị trí tương ứng nút "Play" pill trong kit (32_Dark_anime episode details):
// trailer là nội dung video duy nhất app có (Jikan không có video tập phim,
// xem FEATURES.md mục 4), nên nó xứng đáng ngôn ngữ hình ảnh của một video.
// Bấm vào ĐIỀU HƯỚNG SANG APP YOUTUBE (không nhúng WebView trong app nữa) —
// YouTube đã siết chặn embed player từ phía họ (lỗi "Error 153", xảy ra diện
// rộng không riêng app này), nên nhúng qua WebView không còn đáng tin cậy.
// onClick chỉ gửi DetailEvent — startActivity() thật sự nằm ở DetailEffect.
// OpenYoutube xử lý trong LaunchedEffect của DetailScreen (như 4 effect điều
// hướng khác), không gọi thẳng trong composable này (quy tắc MVI CLAUDE.md).
@Composable
private fun TrailerCard(thumbnailUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .clickable(onClickLabel = "Xem trailer trên YouTube", role = Role.Button, onClick = onClick),
    ) {
        val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
        val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
        AsyncImage(
            model = thumbnailUrl,
            // Trang trí — hành động đã có onClickLabel + label "Xem trailer" bên dưới.
            contentDescription = null,
            // Crop: thumbnail hqdefault là 4:3 kèm letterbox, crop vào khung
            // 16:9 sẽ tự cắt bỏ 2 bar đen trên/dưới.
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient đáy (0->85%, khớp hero header) thay vì scrim phẳng cũ —
        // scrim phẳng 30% không đủ tương phản khi thumbnail nền sáng/trắng
        // (hqdefault của 1 số video chỉ là ảnh placeholder màu nhạt), label
        // "Xem trailer" bị chìm mất. Gradient đáy luôn đảm bảo vùng chữ tối
        // đủ, bất kể độ sáng của thumbnail.
        val background = MaterialTheme.colorScheme.background
        val gradient = remember(background) {
            Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)))
        }
        Box(modifier = Modifier.fillMaxSize().background(gradient))
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(Dimens.IconButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Variation selector ︎: ép render dạng text đơn sắc, một
                // số font/thiết bị sẽ hiện ▶ dạng emoji màu nếu thiếu nó.
                text = "▶︎",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                // clickable ở Box cha đã merge semantics + onClickLabel "Xem
                // trailer" — ẩn glyph này khỏi TalkBack để khỏi đọc thừa
                // "black right-pointing triangle".
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Text(
            text = "Xem trailer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.SpaceMd),
        )
    }
}

@Composable
private fun SynopsisSection(synopsis: String) {
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
        Text(
            text = "Nội dung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ExpandableText(
            text = synopsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
            maxCollapsedLines = 4,
        )
    }
}

// Chỉ preview CHARACTERS_PREVIEW_LIMIT nhân vật đầu — /characters trả về
// TOÀN BỘ nhân vật 1 lần (không phân trang), anime dài có thể tới hàng nghìn
// (One Piece ~1477). "Xem tất cả" mở CharactersScreen riêng (Routes.CHARACTERS)
// có ô tìm kiếm local (lọc trong bộ nhớ, không cần gọi lại API).
private const val CHARACTERS_PREVIEW_LIMIT = 15

// AnimatedVisibility thay vì `if (characters.isEmpty()) return` — characters
// fetch xong SAU detail (gọi tuần tự trong DetailViewModel.loadAll), nên
// section này "mọc" ra giữa chừng khi đang xem màn; animate cho mượt thay vì
// giật cứng.
@Composable
private fun CharactersSection(characters: List<AnimeCharacter>, onSeeAllClick: () -> Unit) {
    AnimatedVisibility(visible = characters.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nhân vật & Seiyuu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (characters.size > CHARACTERS_PREVIEW_LIMIT) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllClick)
                            .padding(Dimens.SpaceXs),
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                items(
                    characters.take(CHARACTERS_PREVIEW_LIMIT),
                    key = { it.malId },
                ) { character -> CharacterItem(character) }
            }
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
            // minLines = maxLines = 2: luôn chiếm đúng 2 dòng dù tên thực tế
            // dài 1 hay 2 dòng — tránh item "nhấp nhô" cao thấp khác nhau
            // trong cùng 1 hàng LazyRow (cùng lý do với EpisodeItem bên dưới).
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        // Luôn render dòng này (rỗng nếu không có seiyuu) để mọi item cùng
        // chiều cao — không dùng voiceActorName?.let {} nữa (ẩn/hiện có điều
        // kiện chính là nguyên nhân nhấp nhô).
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

// Chỉ preview EPISODES_PREVIEW_LIMIT tập đầu trong list ngang — anime dài có
// thể có hàng chục/trăm tập, hiện hết ở LazyRow là UX tệ. "Xem tất cả" mở màn
// EpisodesScreen riêng (Routes.EPISODES) hiện đủ danh sách đã fetch.
private const val EPISODES_PREVIEW_LIMIT = 10

// AnimatedVisibility thay vì early return — cùng lý do với CharactersSection
// (episodes fetch xong sau detail, "mọc" ra giữa chừng khi đang xem màn).
@Composable
private fun EpisodesSection(episodes: List<Episode>, onSeeAllClick: () -> Unit) {
    AnimatedVisibility(visible = episodes.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Các tập",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (episodes.size > EPISODES_PREVIEW_LIMIT) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllClick)
                            .padding(Dimens.SpaceXs),
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(episodes.take(EPISODES_PREVIEW_LIMIT), key = { it.number }) { episode -> EpisodeItem(episode) }
            }
        }
    }
}

@Composable
private fun EpisodeItem(episode: Episode) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(modifier = Modifier.width(Dimens.EpisodeCardWidth)) {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Dimens.RadiusCard)),
        )
        Text(
            text = "Tập ${episode.number}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            // minLines = maxLines = 2: luôn chiếm đúng 2 dòng dù title thực tế
            // dài 1 hay 2 dòng — tránh card "nhấp nhô" cao thấp khác nhau trong
            // cùng 1 hàng LazyRow.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
            // key(): app dùng stale-while-revalidate — nếu detail.relations đổi
            // hình dạng giữa các lần refresh nền, remember theo vị trí slot có
            // thể gán nhầm trạng thái expanded/isOverflowing cũ cho nội dung mới.
            key(group.relation) {
                ExpandableText(text = "${group.relation}: ${group.titles.joinToString(", ")}")
            }
        }
    }
}

// Dùng chung cho Synopsis + từng dòng Relations — thu gọn N dòng + "Xem
// thêm"/"Thu gọn", nhưng chỉ hiện nút toggle khi text THỰC SỰ tràn quá
// maxCollapsedLines (onTextLayout + hasVisualOverflow), tránh hiện "Xem thêm"
// thừa khi nội dung vốn đã đủ ngắn.
@Composable
private fun ExpandableText(text: String, modifier: Modifier = Modifier, maxCollapsedLines: Int = 3) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflowing by remember { mutableStateOf(false) }
    // animateContentSize(): chiều cao Column đổi mượt khi maxLines đổi (thay
    // vì "giật" cứng) lúc bấm Xem thêm/Thu gọn.
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else maxCollapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) isOverflowing = result.hasVisualOverflow },
        )
        if (isOverflowing) {
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
}

// Bộ sưu tập ảnh từ /pictures (poster art các thời kỳ) — tính năng phát sinh
// ngoài kit Animax, không có mockup: LazyRow poster 2:3 (khớp AnimeCard),
// bấm ảnh mở viewer full-screen vuốt ngang. AnimatedVisibility vì pictures
// là lệnh gọi phụ cuối cùng, "mọc" ra sau khi detail đã hiển thị.
@Composable
private fun PicturesSection(pictures: List<String>) {
    // Index ảnh đang mở trong viewer, null = viewer đóng — state thuần UI
    // (giống expanded của ExpandableText), không cần đưa vào DetailState.
    // rememberSaveable: giữ viewer mở đúng ảnh khi xoay màn hình. Đặt NGOÀI
    // AnimatedVisibility để lifecycle viewer không phụ thuộc animation ẩn/hiện.
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    AnimatedVisibility(visible = pictures.isNotEmpty()) {
        Column {
            SectionTitle("Hình ảnh")
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                itemsIndexed(pictures, key = { _, url -> url }) { index, url ->
                    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
                    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
                    AsyncImage(
                        model = url,
                        contentDescription = "Ảnh ${index + 1}",
                        contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter,
                        modifier = Modifier
                            .width(Dimens.CardWidth)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(Dimens.RadiusCard))
                            .clickable(onClickLabel = "Xem ảnh lớn") { viewerIndex = index },
                    )
                }
            }
        }
    }

    // Guard pictures.isNotEmpty + coerceIn: sau process death, viewerIndex
    // được rememberSaveable khôi phục TRƯỚC khi pictures fetch lại xong —
    // không guard sẽ mở viewer rỗng / index vượt biên trong lúc chờ.
    val index = viewerIndex
    if (index != null && pictures.isNotEmpty()) {
        PictureViewerDialog(
            pictures = pictures,
            initialPage = index.coerceIn(0, pictures.lastIndex),
            onDismiss = { viewerIndex = null },
        )
    }
}

@Composable
private fun PictureViewerDialog(pictures: List<String>, initialPage: Int, onDismiss: () -> Unit) {
    // usePlatformDefaultWidth = false: dialog chiếm trọn màn hình cho viewer
    // ảnh, thay vì bị bó trong khung dialog mặc định.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val pagerState = rememberPagerState(initialPage = initialPage) { pictures.size }
            val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
            val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Preload trang kề: ảnh large nặng, không preload thì mỗi lần
                // vuốt user phải nhìn placeholder chờ tải từ đầu.
                beyondViewportPageCount = 1,
                key = { pictures[it] },
            ) { page ->
                AsyncImage(
                    model = pictures[page],
                    contentDescription = "Ảnh ${page + 1}",
                    // Fit (không Crop): viewer phải thấy TRỌN ảnh gốc, đây là
                    // mục đích của việc phóng to.
                    contentScale = ContentScale.Fit,
                    placeholder = placeholderPainter,
                    error = placeholderPainter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.ScreenPadding),
                )
            }

            // Nút đóng — cùng ngôn ngữ hình ảnh với BackButton (tròn, nền mờ).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.SpaceSm)
                    .size(Dimens.IconButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss)
                    .semantics { contentDescription = "Đóng" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    // Box cha đã có contentDescription "Đóng" — ẩn glyph khỏi
                    // TalkBack để khỏi đọc thừa "Đóng, multiplication sign".
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            Text(
                text = "${pagerState.currentPage + 1}/${pictures.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.ScreenPadding),
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
    onSeeAllReviewsClick: () -> Unit,
) {
    // AnimatedVisibility thay vì early return khi cả 2 đều rỗng — cùng lý do
    // với CharactersSection/EpisodesSection (recommendations/reviews fetch
    // xong sau detail, "mọc" ra giữa chừng khi đang xem màn).
    AnimatedVisibility(visible = recommendations.isNotEmpty() || reviews.isNotEmpty()) {
        when {
            recommendations.isNotEmpty() && reviews.isNotEmpty() -> {
                var selectedTab by rememberSaveable { mutableStateOf(DetailTab.RECOMMENDATIONS) }
                Column {
                    DetailTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                    // Crossfade khi đổi tab thay vì "nhảy" cứng giữa 2 nội dung
                    // khác hình dạng (LazyRow vs Column cao thấp khác nhau).
                    // SizeTransform(clip = false) — nếu không, nội dung cao hơn
                    // bị cắt trong lúc animate vì AnimatedContent mặc định clip
                    // theo size nội suy (compose-expert/animation.md mục tab-switch).
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                        label = "detail_tab_content",
                    ) { tab ->
                        when (tab) {
                            DetailTab.RECOMMENDATIONS -> RecommendationsRow(recommendations, onRecommendationClick)
                            DetailTab.REVIEWS -> ReviewsList(reviews, onSeeAllReviewsClick)
                        }
                    }
                }
            }

            recommendations.isNotEmpty() -> Column {
                SectionTitle("Đề xuất tương tự")
                RecommendationsRow(recommendations, onRecommendationClick)
            }

            reviews.isNotEmpty() -> Column {
                SectionTitle("Đánh giá")
                ReviewsList(reviews, onSeeAllReviewsClick)
            }
            // Cả 2 đều rỗng: AnimatedVisibility(visible=false) đã lo phần ẩn,
            // nhánh này chỉ để tường minh chứ không có gì phải render.
            else -> Unit
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

// Dùng TabRow/Tab chuẩn Material3 thay vì tự vẽ Row + gạch chân (bản tự vẽ
// trước đó thiếu track nền + không animate, nhìn "thô") — TabRow tự lo phần
// đo width chia đều và vẽ indicator trượt mượt giữa 2 tab.
@Composable
private fun DetailTabRow(selectedTab: DetailTab, onTabSelected: (DetailTab) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        DetailTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = if (tab == DetailTab.RECOMMENDATIONS) "Đề xuất" else "Đánh giá",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

// reviews đã bị giới hạn số lượng ở AnimeDetailRepository.getReviews() (chỉ
// preview) — luôn hiện "Xem tất cả" vì preview luôn bị cắt, khả năng còn thêm
// review khác qua ReviewsScreen (Paging 3) là gần như chắc chắn.
@Composable
private fun ReviewsList(reviews: List<AnimeReview>, onSeeAllClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        reviews.forEach { review -> ReviewCard(review) }
        // "Xem tất cả" đặt DƯỚI danh sách (khác các section LazyRow ngang có
        // nút ở header) — với list dọc, đọc hết preview rồi mới tới lời mời
        // xem thêm thuận mắt hơn.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Xem tất cả",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(Dimens.SpaceXs),
            )
        }
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
