package com.lambao.animike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.ReviewTag
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.success

/**
 * Card đánh giá dùng chung ReviewsScreen (Paging, live /anime/{id}/reviews)
 * lẫn tab "Đánh giá" ở Detail (preview top-5 từ Room, xem
 * CachedReviewPreviewMapper) — theo yêu cầu user: đồng bộ hiển thị 2 nơi.
 * Field avatar/date/tag/reactions đều nullable nên cả 2 nguồn dữ liệu đều
 * render đúng, chỉ khác lượng field thực có.
 */
@Composable
fun ReviewCard(review: AnimeReview, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val placeholderColor = MaterialTheme.colorScheme.surface
            val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
            AsyncImage(
                model = review.userAvatarUrl,
                // null: username Text ngay bên cạnh đã đủ cho TalkBack.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier.size(Dimens.ReviewAvatarSize).clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.username,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Luôn render dòng này (rỗng nếu không có date) để mọi card
                // cùng chiều cao — cùng lý do với CharacterItem ở DetailScreen.
                Text(
                    text = review.date ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (review.score != null) {
                Text(
                    text = "★ ${review.score}/10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (review.tag != null) {
            ReviewTagBadge(tag = review.tag)
        }
        Text(
            text = review.reviewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
        val overallReactions = review.reactions?.overall
        if (overallReactions != null) {
            Text(
                text = "👍 $overallReactions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// tags mình tự thiết kế (không có mockup) — nhãn tiếng Việt + màu theo mức độ
// khuyến nghị: xanh (success, tái dùng token "ĐANG CHIẾU" ở Detail) cho
// Recommended, tertiary (trung tính) cho Mixed Feelings, error cho Not
// Recommended. Nền màu nhạt (alpha 0.15) + chữ màu đậm — cùng phong cách chip
// đã dùng ở GenreChips.
@Composable
private fun ReviewTagBadge(tag: ReviewTag) {
    val (label, color) = when (tag) {
        ReviewTag.RECOMMENDED -> "Nên xem" to MaterialTheme.colorScheme.success
        ReviewTag.MIXED_FEELINGS -> "Cảm xúc lẫn lộn" to MaterialTheme.colorScheme.tertiary
        ReviewTag.NOT_RECOMMENDED -> "Không nên xem" to MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
