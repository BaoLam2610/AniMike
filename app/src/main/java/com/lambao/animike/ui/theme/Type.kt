package com.lambao.animike.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lambao.animike.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val Default = Typography()

// AniMike type scale — see .claude/skills/animike-design/SKILL.md
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = Default.displayMedium.copy(fontFamily = InterFontFamily),
    // Tên anime ở Detail
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineLarge = Default.headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = Default.headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = Default.headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = Default.titleLarge.copy(fontFamily = InterFontFamily),
    // Section header ("Season Now", "Top Anime")
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = Default.titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = Default.bodyLarge.copy(fontFamily = InterFontFamily),
    // Synopsis, nội dung
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = Default.bodySmall.copy(fontFamily = InterFontFamily),
    // Nút, tab
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = Default.labelMedium.copy(fontFamily = InterFontFamily),
    // Chip genre, meta info (year, type, eps)
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
