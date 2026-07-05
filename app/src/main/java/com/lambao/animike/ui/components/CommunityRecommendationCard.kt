package com.lambao.animike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.CommunityRecommendation
import com.lambao.animike.ui.theme.Dimens

/**
 * Card cho "Đề xuất cộng đồng" (MVP4, /recommendations/anime) — dùng ở cả
 * preview Home (LazyRow, width cố định) lẫn màn "Xem tất cả" (LazyColumn,
 * fillMaxWidth). Khác AnimeCard: 2 poster ghép cặp cạnh nhau (mỗi ảnh bấm
 * riêng để mở đúng Detail của anime đó), không phải 1 poster + score.
 */
@Composable
fun CommunityRecommendationCard(
    recommendation: CommunityRecommendation,
    onAnimeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // 2 dòng cho preview Home (card nhỏ), nhiều hơn cho "Xem tất cả" (đủ chỗ
    // dọc hơn vì fillMaxWidth thay vì width cố định).
    contentMaxLines: Int = 2,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            RecommendationPoster(
                title = recommendation.firstAnimeTitle,
                imageUrl = recommendation.firstAnimeImageUrl,
                onClick = { onAnimeClick(recommendation.firstAnimeId) },
                modifier = Modifier.weight(1f),
            )
            RecommendationPoster(
                title = recommendation.secondAnimeTitle,
                imageUrl = recommendation.secondAnimeImageUrl,
                onClick = { onAnimeClick(recommendation.secondAnimeId) },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = recommendation.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = contentMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "— ${recommendation.username}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecommendationPoster(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = MaterialTheme.colorScheme.surface
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Column(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = imageUrl,
            // null: title Text ngay bên dưới đã đủ cho TalkBack (cùng quy ước
            // showTitle=true của AnimeCard) — tránh đọc trùng lặp.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(Dimens.RadiusChip)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
    }
}
