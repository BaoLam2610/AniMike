package com.lambao.animike.ui.theme

import androidx.compose.ui.graphics.Color

// AniMike design tokens v2 (dark theme) — premium cinema-dark, champagne gold.
// See .claude/skills/animike-design/SKILL.md (v2.0.0) + design/tokens.json
val Background = Color(0xFF08080B)
val Surface = Color(0xFF111013)
val SurfaceVariant = Color(0xFF1B1A1F)
val Outline = Color(0xFF2A2830)
val Primary = Color(0xFFD4AF6A)        // champagne gold
val OnPrimary = Color(0xFF2A1F08)      // nâu đậm — chữ/icon trên nền gold
val Secondary = Color(0xFFE0A9B0)      // rose gold
val OnSecondary = Color(0xFF35171C)
val Tertiary = Color(0xFF8FB0C4)       // dusty blue — info/score hero ★
val OnBackground = Color(0xFFECE6DA)   // warm off-white
val OnSurfaceVariant = Color(0xFF9A9184)
val Error = Color(0xFFD9605C)
val Success = Color(0xFF6FBF8E)        // muted jade — "Airing"

// Huy hiệu xếp hạng top-3 (MVP5 "Top nhân vật") — vàng/bạc/đồng. Không phá
// quy tắc "1 accent/ngữ cảnh" của animike-design vì đây là ribbon XẾP HẠNG
// (ngữ nghĩa cố định như medal), không phải accent UI theo chủ đề. v2: hài hoà
// với accent gold (rankGold sáng hơn Primary để phân biệt trên ribbon).
val RankGold = Color(0xFFE3C06A)
val RankSilver = Color(0xFFC7CDD6)
val RankBronze = Color(0xFFC68B5A)
