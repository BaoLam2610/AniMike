package com.lambao.animike.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeListSource
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.AniMikeTheme
import com.lambao.animike.ui.theme.Dimens

// Số trang hero slider (Season Now) và số item preview mỗi section ngang —
// API/Room vẫn giữ nguyên 25 item/list (không đổi schema cache), UI chỉ
// compose phần cần hiển thị.
private const val HERO_PAGE_COUNT = 5
private const val SECTION_PREVIEW_LIMIT = 10

@Composable
fun HomeScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAnimeList: (AnimeListSource) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
                HomeEffect.NavigateToTopAnime -> onNavigateToAnimeList(AnimeListSource.TOP)
                HomeEffect.NavigateToUpcoming -> onNavigateToAnimeList(AnimeListSource.UPCOMING)
            }
        }
    }

    HomeScreenContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets cho bottom nav — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(HomeEvent.OnPullToRefresh) },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                // Indicator mặc định nằm TopCenter — đúng vùng bar "AniMike"
                // pinned đè lên (bar vẽ sau trong Box). Đẩy xuống dưới bar để
                // thấy rõ trạng thái đang kéo/refresh.
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Dimens.SpaceXxl),
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXl),
                ) {
                    item {
                        // remember: take() tạo List mới mỗi lần gọi — không
                        // remember thì HeroPager nhận instance mới ở MỌI recomposition
                        // (kể cả khi chỉ isRefreshing đổi) và mất khả năng skip.
                        val heroes = remember(state.seasonNow.animeList) {
                            state.seasonNow.animeList.take(HERO_PAGE_COUNT)
                        }
                        HeroPager(
                            heroes = heroes,
                            // Chỉ hiện lỗi khi chưa có cache nào để hero rơi vào — có
                            // cache thì hero vẫn hiện item cũ dù refresh nền vừa lỗi
                            // (stale-while-revalidate, giống AnimeSection).
                            error = state.seasonNow.error.takeIf { state.seasonNow.animeList.isEmpty() },
                            isLoading = state.seasonNow.isLoading,
                            favoriteIds = state.favoriteIds,
                            onHeroClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                            onFavoriteClick = { onEvent(HomeEvent.OnHeroFavoriteClick(it)) },
                            onRetry = { onEvent(HomeEvent.OnRetrySeasonNow) },
                        )
                    }
                    item {
                        AnimeSection(
                            title = "Top Hits Anime",
                            section = state.topAnime,
                            showRank = true,
                            onRetry = { onEvent(HomeEvent.OnRetryTopAnime) },
                            onAnimeClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                            onSeeAllClick = { onEvent(HomeEvent.OnSeeAllTopAnimeClick) },
                        )
                    }
                    item {
                        AnimeSection(
                            title = "Sắp chiếu",
                            section = state.upcoming,
                            onRetry = { onEvent(HomeEvent.OnRetryUpcoming) },
                            onAnimeClick = { onEvent(HomeEvent.OnAnimeClick(it)) },
                            onSeeAllClick = { onEvent(HomeEvent.OnSeeAllUpcomingClick) },
                        )
                    }
                }
            }

            // Header neo cố định đè lên nội dung cuộn (kit Animax đè logo lên
            // hero) — nằm NGOÀI LazyColumn nên không bao giờ cuộn mất.
            HomeTopBar(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Nền bán trong suốt giả lập frosted-glass: nội dung cuộn bên dưới
            // vẫn ánh qua nhẹ. Blur backdrop THẬT cần RenderEffect (API 31+)
            // hoặc thư viện Haze — Compose không blur được "phần phía sau"
            // một cách chính thống; nếu muốn nâng cấp thì thêm Haze sau.
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AniMike",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// Hero slider full-bleed (kit Animax) — tối đa HERO_PAGE_COUNT anime đầu của
// Season Now, vuốt ngang đổi trang + chấm chỉ báo bên dưới. Không hiện
// genres/score vì Anime (list-level) không có field genres — chỉ Detail mới
// fetch đủ; xem quyết định trong docs/ROADMAP.md mục MVP3 UI-2.
@Composable
private fun HeroPager(
    heroes: List<Anime>,
    error: String?,
    isLoading: Boolean,
    favoriteIds: Set<Int>,
    onHeroClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    if (heroes.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(Dimens.HeroHeaderHeight)) {
            when {
                error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SectionError(message = error, onRetry = onRetry)
                }
                // Tải xong nhưng Season Now rỗng thật (không lỗi) — hiện empty
                // state thay vì shimmer vô thời hạn, khớp nhánh else của AnimeSection.
                !isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Chưa có dữ liệu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val shimmerProgress = rememberShimmerProgress()
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect(shimmerProgress))
                }
            }
        }
        return
    }

    Column {
        // pageCount là lambda — PagerState tự cập nhật khi list SWR refresh
        // đổi kích thước, không cần key lại remember.
        val pagerState = rememberPagerState { heroes.size }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(Dimens.HeroHeaderHeight),
            // key theo malId: khi SWR refresh đổi thứ tự Season Now, pager giữ
            // đúng anime đang xem thay vì "hoán đổi" nội dung theo index.
            key = { heroes[it].malId },
        ) { pageIndex ->
            val anime = heroes[pageIndex]
            HeroPage(
                anime = anime,
                isFavorite = anime.malId in favoriteIds,
                onClick = { onHeroClick(anime.malId) },
                onFavoriteClick = { onFavoriteClick(anime.malId) },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs, Alignment.CenterHorizontally),
        ) {
            repeat(heroes.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(Dimens.PagerDotSize)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HeroPage(
    anime: Anime,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
        val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
        AsyncImage(
            model = anime.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
        )
        // Gradient overlay #0B0E14 alpha 0->85% bottom-up (animike-design SKILL.md)
        val background = MaterialTheme.colorScheme.background
        val gradient = remember(background) {
            Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)))
        }
        Box(modifier = Modifier.fillMaxSize().background(gradient))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(onClick = onClick, shape = RoundedCornerShape(Dimens.RadiusButton)) {
                    Text("Xem chi tiết")
                }
                OutlinedButton(onClick = onFavoriteClick, shape = RoundedCornerShape(Dimens.RadiusButton)) {
                    Text(if (isFavorite) "✓ Yêu thích" else "+ Yêu thích")
                }
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
    showRank: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            // Chỉ mời "Xem tất cả" khi thực sự còn nhiều hơn số đang preview.
            if (onSeeAllClick != null && section.animeList.size > SECTION_PREVIEW_LIMIT) {
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
        when {
            // Cache-first (stale-while-revalidate): còn dữ liệu cũ thì luôn hiện
            // ngay, kể cả khi đang refresh nền hoặc refresh vừa lỗi — không để
            // shimmer/error che mất nội dung đã có (jikan-api SKILL.md mục Caching).
            // take(SECTION_PREVIEW_LIMIT): preview 10 item đầu, phần còn lại
            // xem qua màn "Xem tất cả" (AnimeListScreen, Paging 3).
            section.animeList.isNotEmpty() -> LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                itemsIndexed(
                    items = section.animeList.take(SECTION_PREVIEW_LIMIT),
                    key = { _, anime -> anime.malId },
                ) { index, anime ->
                    AnimeCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.malId) },
                        modifier = Modifier.width(Dimens.CardWidth),
                        rank = if (showRank) index + 1 else null,
                    )
                }
            }

            section.isLoading -> {
                // Hoist 1 animation dùng chung cho cả 6 placeholder của section này
                val shimmerProgress = rememberShimmerProgress()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    items(6, key = { it }) {
                        AnimeCardPlaceholder(
                            progress = shimmerProgress,
                            modifier = Modifier.width(Dimens.CardWidth),
                        )
                    }
                }
            }

            section.error != null -> SectionError(message = section.error, onRetry = onRetry)

            else -> Text(
                text = "Chưa có dữ liệu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            )
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
