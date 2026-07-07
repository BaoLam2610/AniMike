package com.lambao.animike.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.lambao.animike.ui.theme.Dimens

// Dùng chung cho Synopsis/Relations ở DetailScreen và "about" ở
// CharacterDetailScreen (MVP5) — thu gọn N dòng + "Xem thêm"/"Thu gọn", chỉ
// hiện nút toggle khi text THỰC SỰ tràn quá maxCollapsedLines (onTextLayout +
// hasVisualOverflow), tránh hiện "Xem thêm" thừa khi nội dung vốn đã đủ ngắn.
@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier, maxCollapsedLines: Int = 3) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflowing by remember { mutableStateOf(false) }
    // animateContentSize(): chiều cao Column đổi mượt khi maxLines đổi (thay
    // vì "giật" cứng) lúc bấm Xem thêm/Thu gọn.
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else maxCollapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) isOverflowing = result.hasVisualOverflow },
        )
        if (isOverflowing) {
            Text(
                text = if (expanded) "Thu gọn" else "Xem thêm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = Dimens.SpaceXs)
                    .clickable { expanded = !expanded },
            )
        }
    }
}
