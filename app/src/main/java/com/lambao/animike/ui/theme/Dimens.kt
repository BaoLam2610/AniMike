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
    val RadiusButton = 10.dp   // v2: nút bo nhẹ hơn (premium)
    val RadiusSheet = 20.dp

    // Scrim gradient đè lên ảnh cover để chữ luôn đọc được (0→này, bottom-up).
    // v2: 0.88 (nền tối hơn #08080B). Rút thành hằng số thay 0.85f rải rác ở
    // DetailScreen/CharacterDetailScreen/NewEpisodeCard.
    const val GradientOverlayAlpha = 0.88f

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

    // "Thống kê" ở Detail (MVP4) — thanh ngang phân bố điểm (score 1-10).
    val ScoreBarHeight = 8.dp
    val ScoreBarLabelWidth = 16.dp
    val ScoreBarPercentWidth = 40.dp

    // Avatar user trong ReviewCard (MVP4) — nhỏ hơn AvatarSize (dùng cho
    // seiyuu/character ở Detail) vì đây chỉ là icon phụ cạnh username, không
    // phải nội dung chính của item.
    val ReviewAvatarSize = 36.dp

    // Tab "Video" ở Detail (MVP4) — card thumbnail 16:9 rộng hơn
    // EpisodeCardWidth (140dp) vì title promo/MV dài hơn ("PV 1 English dub
    // version") + có dòng subtitle bài hát/nghệ sĩ cho MV.
    val VideoCardWidth = 200.dp

    // "Vai diễn lồng tiếng" ở People Detail (MVP5) — card nhóm theo anime:
    // poster 2:3 bên trái + các dòng nhân vật (avatar tròn) bên phải.
    val VoiceRoleGroupPosterWidth = 64.dp
    val VoiceRoleGroupPosterHeight = 96.dp
    val VoiceRoleCharacterBadgeSize = 32.dp

    // Viền mảnh (ring quanh avatar, divider...) — thay cho hardcode 2.dp rải
    // rác (góp ý từ review).
    val BorderThin = 2.dp

    // Logo studio ở Studio Detail (MVP5) — ContentScale.Fit trong card surface,
    // to hơn AvatarSize vì là điểm nhấn chính của màn.
    val StudioLogoSize = 120.dp

    // Nút −/+ tiến độ tập (MVP6 Đợt 2, EpisodesSection) — nhỏ hơn
    // IconButtonSize (40dp) vì nằm INLINE cạnh text, không phải icon toolbar.
    val StepperButtonSize = 28.dp

    // FAB nổi mở màn Debug (chỉ DEBUG build, kéo-thả được) — cỡ FAB nhỏ M3.
    val DebugFabSize = 48.dp
    // Lề đáy vị trí nghỉ của FAB Debug — né bottom nav bar (~80dp) + lề, để FAB
    // không đè lên tab "Danh sách" ở Home (góp ý user).
    val DebugFabBottomPadding = 96.dp
}
