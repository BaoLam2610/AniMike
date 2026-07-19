package com.lambao.animike.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lambao.animike.R

// AniMike type scale v2 — premium: Cormorant (serif) cho display/headline,
// Montserrat (sans) cho mọi phần còn lại. See animike-design SKILL.md v2.0.0.
// Bundle 5 weight local (.ttf trong res/font/): cormorant_semibold, cormorant_medium,
// montserrat_regular, montserrat_medium, montserrat_semibold.

val CormorantFontFamily = FontFamily(
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
)

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
)

// TODO(Đợt 11): xoá InterFontFamily + inter_*.otf sau khi build xanh & không còn tham chiếu.
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Số thứ hạng đè lên AnimeCard ("Top Hits Anime", kit Animax). Giữ style RIÊNG
// (Montserrat SemiBold, cỡ lớn) — KHÔNG serif-hoá theo headlineSmall mới, để số
// rank vẫn to & rõ (quyết định user 2026-07).
val RankNumberTextStyle = TextStyle(
    fontFamily = MontserratFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
)

private val Default = Typography()

val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = CormorantFontFamily),
    displayMedium = Default.displayMedium.copy(fontFamily = CormorantFontFamily),
    // Tên anime ở Detail hero
    displaySmall = TextStyle(
        fontFamily = CormorantFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineLarge = Default.headlineLarge.copy(fontFamily = CormorantFontFamily),
    headlineMedium = Default.headlineMedium.copy(fontFamily = CormorantFontFamily),
    // Tiêu đề lớn (serif) — dùng cho điểm nhấn display, KHÔNG dùng cho số rank
    // (số rank đã có RankNumberTextStyle riêng).
    headlineSmall = TextStyle(
        fontFamily = CormorantFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = Default.titleLarge.copy(fontFamily = MontserratFontFamily),
    // Section header ("Top Hits Anime"...)
    titleMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = Default.titleSmall.copy(fontFamily = MontserratFontFamily),
    bodyLarge = Default.bodyLarge.copy(fontFamily = MontserratFontFamily),
    // Synopsis, nội dung
    bodyMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = Default.bodySmall.copy(fontFamily = MontserratFontFamily),
    // Nút, tab
    labelLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = Default.labelMedium.copy(fontFamily = MontserratFontFamily),
    // Chip genre, meta info (year, type, eps)
    labelSmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
