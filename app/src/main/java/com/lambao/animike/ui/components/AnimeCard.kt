package com.lambao.animike.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.ui.theme.Dimens

// Rộng cố định 120dp khi dùng trong LazyRow (Home/Detail), fillMaxWidth khi
// dùng trong grid (Search) — width do modifier của call site quyết định.
@Composable
fun AnimeCard(anime: Anime, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)

    Column(modifier = modifier.clickable(onClick = onClick)) {
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
        Text(
            text = anime.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        ) {
            Text(
                text = "★ ${anime.score}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            if (anime.year != null) {
                Text(
                    text = "· ${anime.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
