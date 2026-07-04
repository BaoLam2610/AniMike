package com.lambao.animike.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.theme.Dimens

// Rộng cố định 120dp khi dùng trong LazyRow (Home/Detail), fillMaxWidth khi
// dùng trong grid (Search) — width do modifier của call site quyết định.
// Score badge đè góc trên-trái ảnh theo kit Animax (docs/UI, MVP3 UI-1).
@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Số thứ hạng đè góc dưới-trái ảnh — chỉ dùng cho section "Top Hits"
    // trên Home (kit Animax), null ở mọi nơi khác.
    rank: Int? = null,
) {
    // ColorPainter không override equals() — remember để tránh AsyncImage coi
    // placeholder/error/fallback là "đổi" ở mỗi recomposition khi list cuộn.
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = anime.imageUrl,
                // Ảnh trang trí — title hiển thị ngay bên dưới đã đủ cho TalkBack,
                // tránh đọc trùng lặp 2 lần.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(Dimens.RadiusCard)),
            )
            // Mapper trả "N/A" khi Jikan không có score — ẩn badge thay vì hiện "N/A"
            if (anime.score != "N/A") {
                ScoreBadge(
                    score = anime.score,
                    modifier = Modifier.padding(Dimens.SpaceSm).align(Alignment.TopStart),
                )
            }
            if (rank != null) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Dimens.SpaceSm),
                )
            }
        }
        Text(
            text = anime.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        if (anime.year != null) {
            Text(
                text = "${anime.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpaceXs),
            )
        }
    }
}

@Composable
private fun ScoreBadge(score: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = score,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
fun AnimeCardPlaceholder(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .shimmerEffect(progress),
    )
}
