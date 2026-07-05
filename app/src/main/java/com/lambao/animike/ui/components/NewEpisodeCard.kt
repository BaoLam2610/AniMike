package com.lambao.animike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.NewEpisodeRelease
import com.lambao.animike.ui.theme.Dimens

/**
 * Card cho "Tập mới phát hành" (MVP4, /watch/episodes/popular) — dùng ở cả
 * preview Home lẫn màn "Xem tất cả". Khác AnimeCard: title + nhãn "Episode N"
 * đè lên ảnh (thay vì title/year bên dưới), KHÔNG có score badge vì endpoint
 * này không trả field score (không phải thiếu ngẫu nhiên — cấu trúc response
 * cố định không có, xem WatchEpisodeEntryDto).
 */
@Composable
fun NewEpisodeCard(release: NewEpisodeRelease, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = release.imageUrl,
            // null: title + nhãn tập đã hiển thị trực quan bên dưới (Column),
            // Compose tự merge semantics từ 2 Text đó lên clickable cha (giống
            // AnimeCard) — không tự tay ghép contentDescription để tránh lệch
            // với text hiển thị nếu sau này chỉ sửa 1 trong 2 Text.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient đáy 0->85% (khớp hero/trailer card) để text luôn đọc được
        // bất kể ảnh sáng/tối.
        val background = MaterialTheme.colorScheme.background
        val gradient = remember(background) {
            Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)))
        }
        Box(modifier = Modifier.fillMaxSize().background(gradient))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            // Bổ sung theo yêu cầu user — chỉ hiện ảnh + nhãn tập thì không
            // biết được bộ phim tên gì, đặc biệt các bìa không có logo/tên
            // đọc được (khác ví dụ Naruto/One Piece trong kit). maxLines = 1
            // (khác quy tắc 2 dòng của AnimeCard) vì đây là overlay đè lên
            // ảnh cùng episodeLabel bên dưới — không gian dọc hẹp hơn khu vực
            // title/year nằm riêng bên dưới ảnh của AnimeCard.
            Text(
                text = release.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = release.episodeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
