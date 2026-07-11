package com.lambao.animike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lambao.animike.ui.theme.Dimens

// Nút nổi "cuộn lên đầu" — dùng chung cho mọi màn hình có LazyColumn dài
// (Detail, Person Detail...). Nền primary (khác BackButton/FavoriteButton
// dùng nền surface bán trong suốt vì 2 nút đó luôn nổi trên hero, còn nút này
// nổi trên nội dung cuộn nên cần tương phản mạnh hơn để không lẫn vào nền).
// Trích xuất từ DetailScreen.kt khi Person Detail cũng cần (2 nơi dùng).
@Composable
fun ScrollToTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.IconButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Cuộn lên đầu trang" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "↑",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
