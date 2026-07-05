package com.lambao.animike.ui.theme

import androidx.compose.ui.unit.dp

// AniMike spacing & shape tokens — see .claude/skills/animike-design/SKILL.md
object Dimens {
    // Spacing scale
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp
    val SpaceXxl = 32.dp

    // Semantic aliases
    val ScreenPadding = SpaceLg
    val CardGap = SpaceMd
    val CardWidth = 120.dp

    // Corner radius
    val RadiusChip = 8.dp
    val RadiusCard = 12.dp
    val RadiusButton = 12.dp
    val RadiusSheet = 20.dp

    // Hero header — dùng chung cho Detail (ảnh cover full-width) và Home (hero
    // section, kit Animax MVP3 UI-2)
    val HeroHeaderHeight = 360.dp
    val IconButtonSize = 40.dp
    val AvatarSize = 64.dp

    // Schedules — dòng ngang (thumbnail + title + meta), kit Animax MVP3 UI-3
    val ScheduleThumbnailWidth = 96.dp
    val ScheduleRowHeight = 88.dp

    // Detail — thẻ tập phim trong EpisodesSection, kit Animax MVP3 UI-4
    val EpisodeCardWidth = 140.dp

    // ReviewsScreen — chiều cao placeholder shimmer cho review card
    val ReviewCardPlaceholderHeight = 96.dp

    // Home hero slider — chấm chỉ báo trang (page indicator dot)
    val PagerDotSize = 8.dp

    // "Đề xuất cộng đồng" (MVP4) — card rộng hơn AnimeCard vì chứa 2 poster
    // ghép cặp cạnh nhau + đoạn lý do bên dưới.
    val CommunityRecommendationCardWidth = 240.dp
}
