package com.lambao.animike.ui.favorites

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.domain.model.WatchStatus
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.emoji
import com.lambao.animike.ui.components.label
import com.lambao.animike.ui.components.statusColor
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
                text = "Danh sách",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )

            when {
                state.entries.isEmpty() -> EmptyLibraryContent()

                else -> {
                    LibraryFilterChips(state = state, onFilterSelected = { onEvent(FavoritesEvent.OnFilterSelected(it)) })
                    // Gọi getter filteredEntries đúng 1 lần/recomposition —
                    // filter O(n) chạy 2 lần nếu vừa check isEmpty vừa truyền
                    // xuống grid (góp ý từ review).
                    val filtered = state.filteredEntries
                    if (filtered.isEmpty()) {
                        EmptyFilterContent()
                    } else {
                        LibraryGrid(
                            entries = filtered,
                            onAnimeClick = { onEvent(FavoritesEvent.OnAnimeClick(it)) },
                        )
                    }
                }
            }
        }
    }
}

// Key tường minh cho LazyRow — KHÔNG dùng toString() vì R8 obfuscate tên
// class của data object/data class, key lưu trong saved state sẽ lệch giữa
// 2 bản build khác mapping (góp ý từ review).
private val LibraryFilter.key: String
    get() = when (this) {
        LibraryFilter.All -> "all"
        LibraryFilter.Favorite -> "favorite"
        is LibraryFilter.ByStatus -> "status_${status.name}"
    }

// Hàng chip lọc: Tất cả / ♥ Yêu thích / 5 trạng thái xem — chip trạng thái
// CHỈ hiện khi có ít nhất 1 anime (row không dài vô ích khi user chưa dùng
// tracking), mỗi chip mang màu ngữ nghĩa riêng (WatchStatusUi) + count.
@Composable
private fun LibraryFilterChips(state: FavoritesState, onFilterSelected: (LibraryFilter) -> Unit) {
    val filters = remember(state.entries) {
        buildList {
            add(LibraryFilter.All)
            if (state.count(LibraryFilter.Favorite) > 0) add(LibraryFilter.Favorite)
            WatchStatus.entries.forEach { status ->
                if (state.count(LibraryFilter.ByStatus(status)) > 0) add(LibraryFilter.ByStatus(status))
            }
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(filters, key = { it.key }) { filter ->
            LibraryFilterChip(
                filter = filter,
                count = state.count(filter),
                selected = filter == state.filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(filter: LibraryFilter, count: Int, selected: Boolean, onClick: () -> Unit) {
    // Màu accent theo ngữ nghĩa: Tất cả = primary, Yêu thích = secondary
    // (khớp màu ♥ của FavoriteButton), trạng thái = màu riêng từng trạng thái.
    val accent: Color = when (filter) {
        LibraryFilter.All -> MaterialTheme.colorScheme.primary
        LibraryFilter.Favorite -> MaterialTheme.colorScheme.secondary
        is LibraryFilter.ByStatus -> filter.status.statusColor()
    }
    val chipLabel = when (filter) {
        LibraryFilter.All -> "Tất cả"
        LibraryFilter.Favorite -> "♥ Yêu thích"
        is LibraryFilter.ByStatus -> "${filter.status.emoji} ${filter.status.label}"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(if (selected) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                onClickLabel = "Lọc: $chipLabel, $count anime",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = chipLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryGrid(entries: List<LibraryEntry>, onAnimeClick: (Int) -> Unit) {
    // 2 cột poster lớn (kit Animax MVP3 UI-5) — khác 3 cột nhỏ của
    // Characters/Search vì đây là grid poster chính, không phải preview.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(entries, key = { it.anime.malId }) { entry ->
            LibraryCard(entry = entry, onClick = { onAnimeClick(entry.anime.malId) })
        }
    }
}

// AnimeCard + 2 badge overlay: trạng thái xem (emoji màu, góc dưới-trái) +
// ♥ nếu yêu thích (góc dưới-phải) — góc trên-trái đã có score badge của
// AnimeCard nên dùng cạnh dưới cho metadata cá nhân.
@Composable
private fun LibraryCard(entry: LibraryEntry, onClick: () -> Unit) {
    Box {
        AnimeCard(
            anime = entry.anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            showTitle = false,
        )
        if (entry.status != null) {
            val statusColor = entry.status.statusColor()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Dimens.SpaceSm)
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs)
                    // TalkBack đọc tên trạng thái thay vì glyph thô — cùng
                    // fix a11y với TopCharacterCard/StatChip.
                    .clearAndSetSemantics { contentDescription = entry.status.label },
            ) {
                Text(
                    text = "${entry.status.emoji} ${entry.status.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
        }
        if (entry.isFavorite && entry.status != null) {
            // ♥ nhỏ góc dưới-phải CHỈ khi anime vừa yêu thích vừa có trạng
            // thái — khi chỉ yêu thích (không status) thì việc nó nằm trong
            // tab này đã tự nói lên, không cần badge thừa.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.SpaceSm)
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs)
                    .clearAndSetSemantics { contentDescription = "Đã yêu thích" },
            ) {
                Text(
                    text = "♥",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.padding(horizontal = Dimens.SpaceXxl),
        ) {
            // Icon bookmark khớp với icon tab (AniMikeNavHost) — không dùng trái
            // tim vì glyph đó đã đại diện cho hành động "Yêu thích" ở Home/Detail.
            Text(text = "🔖", style = MaterialTheme.typography.displayMedium)
            Text(
                text = "Danh sách trống",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Dimens.SpaceMd),
            )
            Text(
                text = "Yêu thích hoặc chọn trạng thái xem cho một anime để nó xuất hiện ở đây",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyFilterContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Không có anime nào trong mục này",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
