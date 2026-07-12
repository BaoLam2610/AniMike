package com.lambao.animike.ui.studiodetail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.debug.AppLog
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.StudioDetail
import com.lambao.animike.domain.model.StudioExternalLink
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.AnimeCardPlaceholder
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.ExpandableText
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.theme.Dimens

@Composable
fun StudioDetailScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: StudioDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is StudioDetailEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)

                is StudioDetailEffect.OpenExternalUrl -> {
                    // URL đã được mapper lọc chỉ http/https — mở browser ngoài
                    // (cùng cách DetailEffect.OpenExternalUrl của "Xem trên...").
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                    } catch (e: ActivityNotFoundException) {
                        AppLog.w("StudioDetailScreen", "Không có app xử lý được link studio", e)
                    }
                }
            }
        }
    }

    StudioDetailScreenContent(
        state = state,
        pagingItems = pagingItems,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun StudioDetailScreenContent(
    state: StudioDetailState,
    pagingItems: LazyPagingItems<Anime>,
    onBackClick: () -> Unit,
    onEvent: (StudioDetailEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ insets.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                // Cache-first: có studio (từ Room) thì luôn hiện header + grid.
                state.studio != null -> StudioDetailContent(
                    studio = state.studio,
                    pagingItems = pagingItems,
                    onEvent = onEvent,
                )

                state.isLoading -> LoadingContent()

                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(StudioDetailEvent.OnRetry) },
                )

                else -> LoadingContent()
            }

            BackButton(onClick = onBackClick, modifier = Modifier.padding(Dimens.SpaceSm))
        }
    }
}

@Composable
private fun StudioDetailContent(
    studio: StudioDetail,
    pagingItems: LazyPagingItems<Anime>,
    onEvent: (StudioDetailEvent) -> Unit,
) {
    // Header (logo/stat/external/about) là các item full-span ĐẦU grid, cuộn
    // cùng danh sách anime — cùng pattern ReviewsScreen (statistics là item đầu
    // của LazyColumn paged). span = maxLineSpan ⇒ chiếm trọn 3 cột.
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            StudioHeader(
                studio = studio,
                onExternalLinkClick = { onEvent(StudioDetailEvent.OnExternalLinkClick(it)) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Anime đã sản xuất",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = Dimens.SpaceSm),
            )
        }

        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.malId },
            contentType = pagingItems.itemContentType { "anime" },
        ) { index ->
            pagingItems[index]?.let { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onEvent(StudioDetailEvent.OnAnimeClick(anime.malId)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Lần tải đầu danh sách (chưa có item) — shimmer 1 hàng full-span.
        if (pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val shimmerProgress = rememberShimmerProgress()
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                    repeat(3) {
                        AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Lỗi tải trang đầu (chưa có item nào) — nút thử lại full-span.
        if (pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceLg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    Text(
                        text = error.toAppError().toUserMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { pagingItems.retry() }) { Text("Thử lại") }
                }
            }
        }

        // Studio không có anime nào (rỗng thật, không phải đang tải/lỗi).
        if (pagingItems.loadState.refresh is LoadState.NotLoading && pagingItems.itemCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Chưa có anime nào",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.SpaceMd),
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val shimmerProgress = rememberShimmerProgress()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                ) {
                    repeat(3) {
                        AnimeCardPlaceholder(progress = shimmerProgress, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceMd), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { pagingItems.retry() }) {
                        Text("Tải thêm thất bại — Thử lại")
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Dimens.SpaceLg)) }
    }
}

// Logo là điểm nhấn (KHÁC hero ảnh full-bleed của các Detail khác) — logo
// studio thường nền trong suốt/vuông nên đặt trên card `surface` bo góc,
// ContentScale.Fit (không Crop) để không méo/cắt logo. Quyết định thiết kế
// đã thống nhất (docs/ROADMAP.md MVP5 mục 3).
@Composable
private fun StudioHeader(studio: StudioDetail, onExternalLinkClick: (String) -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusCard))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Dimens.SpaceLg),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = studio.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier.size(Dimens.StudioLogoSize),
            )
        }

        Text(
            text = studio.name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        StudioStatRow(studio = studio)

        if (studio.externalLinks.isNotEmpty()) {
            ExternalLinksRow(links = studio.externalLinks, onLinkClick = onExternalLinkClick)
        }

        if (!studio.about.isNullOrBlank()) {
            ExpandableText(
                text = studio.about,
                modifier = Modifier.fillMaxWidth(),
                maxCollapsedLines = 4,
            )
        }
    }
}

// Số liệu nhanh: tổng anime (nhấn primary, số to nhất vì là "quy mô" studio) +
// năm thành lập + favorites — mỗi cái 1 chip nền primary-nhạt (cùng ngôn ngữ
// StatChip của People Detail). Ẩn chip nào không có dữ liệu.
@Composable
private fun StudioStatRow(studio: StudioDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        // description tách khỏi label hiển thị — TalkBack đọc câu đầy đủ có
        // nghĩa thay vì "phim 329 anime" / "trái tim 6393" (glyph emoji đọc
        // thô), phát hiện qua review.
        if (studio.animeCount > 0) {
            StatChip(icon = "🎬", label = "${studio.animeCount} anime", description = "${studio.animeCount} anime đã sản xuất")
        }
        if (studio.establishedYear != null) {
            StatChip(icon = "📅", label = "Est. ${studio.establishedYear}", description = "Thành lập năm ${studio.establishedYear}")
        }
        if (studio.favorites > 0) {
            StatChip(icon = "♥", label = "${studio.favorites}", description = "${studio.favorites} lượt thích")
        }
    }
}

@Composable
private fun StatChip(icon: String, label: String, description: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs)
            // clearAndSetSemantics: đọc đúng 1 câu `description`, bỏ qua glyph
            // emoji + label rời (nếu không TalkBack ghép "trái tim, 6393").
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(text = icon, style = MaterialTheme.typography.labelLarge)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ExternalLinksRow(links: List<StudioExternalLink>, onLinkClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        items(links, key = { it.url }) { link ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        onClickLabel = "Mở ${link.name}",
                        role = Role.Button,
                        onClick = { onLinkClick(link.url) },
                    )
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
            ) {
                Text(
                    text = "🔗 ${link.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
