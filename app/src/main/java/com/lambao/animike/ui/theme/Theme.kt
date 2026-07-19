package com.lambao.animike.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AniMikeColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,          // v2: nâu đậm trên nền gold (không còn Background)
    secondary = Secondary,
    onSecondary = OnSecondary,      // v2: nâu-đỏ trên nền rose gold
    tertiary = Tertiary,
    onTertiary = Background,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    // Card/BottomNav "Nền `surface`" theo animike-design — map để component
    // M3 (Card, NavigationBar...) dùng đúng nền mặc định mà không cần override thủ công.
    surfaceContainer = Surface,
    surfaceContainerLow = Background,
    surfaceContainerHigh = SurfaceVariant,
    outline = Outline,              // v2: hairline border riêng (không còn dùng OnSurfaceVariant)
    outlineVariant = SurfaceVariant,
    error = Error,
    onError = Background,
)

// Token ngoài Material colorScheme (trạng thái "Airing")
val ColorScheme.success: Color
    get() = Success

// Huy hiệu xếp hạng top-3 (MVP5 "Top nhân vật") — ngoài Material colorScheme
// vì là màu ngữ nghĩa cố định (medal), không map vào role M3 nào.
val ColorScheme.rankGold: Color
    get() = RankGold

// Alias semantic cho trạng thái "cảnh báo nhẹ" (VD "Tạm dừng" của MVP6
// tracking) — cùng giá trị RankGold nhưng tách tên để đổi màu medal sau này
// không vô tình kéo theo màu warning (góp ý từ review).
val ColorScheme.warning: Color
    get() = RankGold
val ColorScheme.rankSilver: Color
    get() = RankSilver
val ColorScheme.rankBronze: Color
    get() = RankBronze

// Dark-first: v1 chỉ có dark theme (animike-design SKILL.md)
@Composable
fun AniMikeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AniMikeColorScheme,
        typography = Typography,
        content = content,
    )
}
