package com.lambao.animike.ui.reviewdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.lambao.animike.domain.model.ReviewReactions
import com.lambao.animike.domain.model.ReviewTag
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.success

/**
 * Xem đầy đủ 1 review (avatar/date/tag/reactions/text KHÔNG bị maxLines cắt).
 * KHÔNG tự có ViewModel — nhận thẳng `review` làm tham số, vì có 2 nơi mở màn
 * này với 2 nguồn dữ liệu khác nhau (ReviewsScreen: ReviewsViewModel.selectedReview;
 * tab "Đánh giá" ở Detail: DetailViewModel.selectedReview) — Paging 3 không có
 * cách tra cứu "item theo id" nên mỗi nơi tự lưu review đang xem vào state của
 * chính ViewModel đó, và composable() ở AniMikeNavHost đọc đúng state rồi
 * truyền xuống đây, không truyền qua nav argument.
 */
@Composable
fun ReviewDetailScreen(review: AnimeReview?, onBackClick: () -> Unit) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Chi tiết đánh giá",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (review == null) {
                // Lý thuyết không xảy ra (chỉ điều hướng tới đây sau khi đã
                // set selectedReview) — phòng thủ thay vì crash nếu có.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Không tìm thấy đánh giá",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ReviewDetailBody(review = review)
            }
        }
    }
}

@Composable
private fun ReviewDetailBody(review: AnimeReview) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
            val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
            AsyncImage(
                model = review.userAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier.size(Dimens.AvatarSize).clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (review.date != null) {
                    Text(
                        text = review.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (review.score != null) {
                Text(
                    text = "★ ${review.score}/10",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        if (review.tag != null) {
            ReviewDetailTagBadge(tag = review.tag)
        }

        // KHÔNG maxLines — đây chính là lý do màn này tồn tại (ReviewCard ở
        // ReviewsScreen giới hạn 6 dòng để danh sách gọn, review dài phải mở
        // đây mới đọc hết được).
        Text(
            text = review.reviewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (review.reactions != null) {
            ReviewReactionsBreakdown(reactions = review.reactions)
        }
    }
}

@Composable
private fun ReviewDetailTagBadge(tag: ReviewTag) {
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

// 7 loại reaction chi tiết (khác ReviewCard ở ReviewsScreen chỉ hiện "overall"
// gộp) — mỗi loại 1 emoji làm icon (cùng quy ước dùng ký tự Unicode thay vector
// icon của toàn app, VD bottom nav/RandomAnimeCard).
@Composable
private fun ReviewReactionsBreakdown(reactions: ReviewReactions) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = "Lượt tương tác",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val rows = listOf(
            "👍 Hữu ích" to reactions.nice,
            "❤️ Rất thích" to reactions.loveIt,
            "😂 Buồn cười" to reactions.funny,
            "❓ Khó hiểu" to reactions.confusing,
            "💡 Nhiều thông tin" to reactions.informative,
            "✍️ Viết tốt" to reactions.wellWritten,
            "🎨 Sáng tạo" to reactions.creative,
        )
        rows.forEach { (label, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
