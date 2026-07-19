package com.lambao.animike.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.TopCharacter
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.rankBronze
import com.lambao.animike.ui.theme.rankGold
import com.lambao.animike.ui.theme.rankSilver
import java.util.Locale

// Dùng chung cho hàng ngang "Nhân vật nổi bật" (Home) và lưới "Top nhân vật"
// ("Xem tất cả"). Width do modifier call-site quyết định (fixed ở LazyRow,
// fillMaxWidth ở grid) — giống AnimeCard. rank != null (1..3) mới hiện ribbon
// huy hiệu (chỉ dùng ở lưới xếp hạng, null ở preview Home).
@Composable
fun TopCharacterCard(
    character: TopCharacter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = character.imageUrl,
                // Name Text bên dưới đã đủ cho TalkBack — ảnh null tránh đọc
                // trùng (giống AnimeCard showTitle=true).
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(Dimens.RadiusCard))
                    // v2: viền hairline "khung tranh" — đồng bộ với AnimeCard.
                    .border(
                        BorderStroke(Dimens.BorderHairline, MaterialTheme.colorScheme.outline),
                        RoundedCornerShape(Dimens.RadiusCard),
                    ),
            )
            if (rank != null && rank in 1..3) {
                RankRibbon(rank = rank, modifier = Modifier.align(Alignment.TopStart).padding(Dimens.SpaceSm))
            }
            FavoritesBadge(
                favorites = character.favorites,
                modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.SpaceSm),
            )
        }
        Text(
            text = character.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
    }
}

@Composable
private fun RankRibbon(rank: Int, modifier: Modifier = Modifier) {
    val medalColor = when (rank) {
        1 -> MaterialTheme.colorScheme.rankGold
        2 -> MaterialTheme.colorScheme.rankSilver
        else -> MaterialTheme.colorScheme.rankBronze
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(medalColor)
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs)
            // TalkBack đọc "Hạng 1" thay vì "thăng, 1" (glyph # đọc thô) — cùng
            // fix a11y với StudioDetail StatChip.
            .clearAndSetSemantics { contentDescription = "Hạng $rank" },
    ) {
        Text(
            // Chữ tối trên nền huy hiệu sáng (vàng/bạc/đồng) — dùng background
            // (nền tối app) làm màu chữ để tương phản đủ, không dùng onX vì
            // medal color không phải role M3.
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun FavoritesBadge(favorites: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            // Nền surface bán trong suốt để chữ đọc được trên ảnh bất kỳ.
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs)
            // TalkBack đọc "6393 lượt thích" thay vì "trái tim, 6.4 K".
            .clearAndSetSemantics { contentDescription = "$favorites lượt thích" },
    ) {
        Text(
            text = "♥ ${formatFavorites(favorites)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// 180028 -> "180K", 6393 -> "6.4K", 329 -> "329" (bỏ ".0" thừa).
private fun formatFavorites(n: Int): String {
    fun trim(value: Double, suffix: String): String {
        val s = String.format(Locale.US, "%.1f", value)
        return (if (s.endsWith(".0")) s.dropLast(2) else s) + suffix
    }
    return when {
        n >= 1_000_000 -> trim(n / 1_000_000.0, "M")
        n >= 1_000 -> trim(n / 1_000.0, "K")
        else -> n.toString()
    }
}
