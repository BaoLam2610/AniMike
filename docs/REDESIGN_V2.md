# AniMike — Kế hoạch Redesign v2.0.0 (Premium cinema-dark, champagne gold)

Đợt nâng cấp **da/visual thuần**: đổi màu, font, độ bo, accent, icon, motion —
**không thêm endpoint, không đổi bố cục/luồng**, giữ nguyên kit Animax (`docs/UI/`)
và phạm vi dữ liệu Jikan. Spec màu/typography: `.claude/skills/animike-design/SKILL.md`
(v2.0.0) + `design/tokens.json`.

Nguyên tắc rollout: mỗi đợt **xong hẳn (build + review + commit) mới sang đợt sau**
— đúng tiền lệ MVP 3 / MVP 6.

## Phát hiện then chốt (khi khảo sát code)

- **Kỷ luật token tuyệt đối**: 0 chỗ hardcode `Color(0xFF…)` ngoài `ui/theme/`.
  Gần như tất cả đọc `MaterialTheme.colorScheme.*` / `typography.*` / `Dimens`.
  → **Đợt 1 (đổi Color/Theme/Type/Dimens) tự reskin ~80% toàn app**; các đợt sau
  chủ yếu verify tương phản + tinh chỉnh chi tiết luxury, không viết lại.
- **App code vẫn là v1**: `Color.kt` (tím `#8B5CF6`, bg `#0B0E14`), `Type.kt` (Inter),
  `Theme.kt` (dark-only, `onPrimary = Background`), `colors.xml` (`#0B0E14`). Chỉ
  spec/tokens.json đã là v2.
- **Icon hiện là glyph/emoji trong `Text()`** (VD bottom bar `icon = { Text(item.icon) }`);
  0 lần dùng `Icons.` / `painterResource`. Thay icon vector là 1 đợt lớn, độc lập.
- **Màu ngữ nghĩa mở rộng** (`warning/rankGold/rankSilver/rankBronze/success`) là
  `val ColorScheme.x get() = <hằng số cố định>` — KHÔNG đọc theo scheme → khi thêm
  light mode phải chuyển thành scheme-aware.
- **Gradient overlay `0.85f` hardcode** ở `DetailScreen`, `CharacterDetailScreen`,
  `NewEpisodeCard` → đổi `0.88` và rút thành hằng số `Dimens.GradientOverlayAlpha`.
- **Xung đột `headlineSmall`**: hiện dùng cho **số thứ hạng đè AnimeCard** (Inter Bold
  28sp), nhưng v2 định nghĩa lại = Cormorant Medium 20sp. → xem Quyết định #1.
- `Dimens.RadiusButton = 12dp` nhưng v2 = **10dp**. Chưa có `ThemeMode`/DataStore/
  toggle → light mode là hạ tầng mới (đợt 9).

## Quyết định đã chốt (user, 2026-07)

1. **Số rank giữ style riêng** — KHÔNG dùng `headlineSmall` mới (Cormorant). Tạo 1
   text style số riêng (**Montserrat Bold**, giữ cỡ lớn ~28sp) cho số thứ hạng đè
   card, để không bị serif-hoá/thu nhỏ.
2. **Bundle font tối thiểu 5 weight**: Cormorant SemiBold + Medium; Montserrat
   Regular + Medium + SemiBold. (Số rank Bold có thể dùng Montserrat SemiBold hoặc
   thêm 1 weight Bold nếu cần đậm hơn — cân nhắc lúc làm.) Giữ Inter tạm tới khi
   build xanh, **xoá Inter ở Đợt 11**.
3. **Light mode để Đợt 9** (không làm sớm). Dark v2 xong trước, light + toggle +
   DataStore + Settings + WCAG tách riêng.

## Các đợt

| # | Đợt | Mục tiêu | Review |
|---|---|---|---|
| 1 | **Nền tảng theme** | Color/Theme/Type/Dimens + bundle 5 font + splash color. Sau đợt này toàn app gold+serif ở dark. | compose-reviewer + accessibility-reviewer |
| 2 | **Components dùng chung** | AnimeCard/ScoreBadge, ReviewCard, TopCharacterCard, NewEpisodeCard, CommunityRecommendationCard, ScrollToTopButton, Shimmer, BackButton, WatchStatusUi + chip/badge hairline | compose-reviewer + accessibility-reviewer |
| 3 | **Home** | Hero full-bleed, Top Hits rank overlay (style số riêng), section header Montserrat | compose-reviewer + accessibility-reviewer |
| 4 | **Detail** | Hero Cormorant `displaySmall`, meta chips, score badge hero (★ tertiary), gradient 0.88, back/favorite pin | compose-reviewer + accessibility-reviewer |
| 5 | **Search + Sort/Filter** | Chip filter, ô search `surfaceVariant`, nút Apply/Reset primary | compose-reviewer + accessibility-reviewer |
| 6 | **Library / Favorites** | Grid poster lớn, score badge, tab "Danh sách" | compose-reviewer |
| 7 | **Browse / Calendar** | Segmented control, chip ngày, list dọc thumbnail (schedules + seasonarchive + browse) | compose-reviewer + accessibility-reviewer |
| 8 | **Các màn detail phụ** | characterdetail, persondetail, studiodetail, topcharacters, characters, reviews, reviewdetail, episodes, newepisodes, communityrecommendations | compose-reviewer |
| 9 | **Light mode + toggle** | lightColorScheme, làm `warning/rank*/success` scheme-aware, ThemeMode + DataStore + màn Settings, `values-night/colors.xml` | accessibility-reviewer (chính) + compose-reviewer |
| 10 | **Icon vector riêng** | Thiết kế bộ SVG (skill `ui-ux-pro-max:design`) → vector drawable → thay glyph/emoji ở bottom bar + toàn app | compose-reviewer + accessibility-reviewer (target ≥48dp) |
| 11 | **Polish motion (3/10)** | Rà motion nhẹ, hạn chế shadow nặng, **xoá Inter + hằng số thừa** | compose-reviewer (compose-android nếu jank) |

**Đợt 2 — hoàn thành ✅ (2026-07)**: 9/9 file xử lý xong. Thực tế: chỉ
`NewEpisodeCard.kt` cần sửa code thật (gradient `0.85f`→`Dimens.GradientOverlayAlpha`
0.88f) — 8 file còn lại tự đúng nhờ đọc `MaterialTheme.colorScheme.*`/extension từ
trước. **Quyết định khác kế hoạch gốc**: viền hairline làm NGAY (không hoãn sang
Đợt 11) — thêm token `Dimens.BorderHairline` (1dp) + áp đồng thời cho cả 3 nơi
(tránh nửa vời): `AnimeCard`, `TopCharacterCard`, 2 poster con trong
`CommunityRecommendationCard`, viền màu `colorScheme.outline`.

## File theo từng đợt

**Đợt 1 — Nền tảng**
- `app/src/main/res/font/` — **thêm** Cormorant (SemiBold, Medium) + Montserrat
  (Regular, Medium, SemiBold).
- `ui/theme/Color.kt` — thay hằng số sang hex dark v2; thêm `OnPrimary` (nâu),
  `OnSecondary`, `Outline`, `Warning`; cập nhật `RankGold/Silver/Bronze` sang v2.
- `ui/theme/Theme.kt` — remap `darkColorScheme`: `onPrimary = OnPrimary` (nâu, bỏ
  `Background`), `onSecondary = OnSecondary`, `outline = Outline`.
- `ui/theme/Type.kt` — khai `CormorantFontFamily` + `MontserratFontFamily`; gán
  `displaySmall` 26sp Cormorant SemiBold, `headlineSmall` Cormorant Medium 20sp,
  `titleMedium`/`bodyMedium`/`labelLarge`/`labelSmall` sang Montserrat; **thêm text
  style số rank riêng (Montserrat Bold ~28sp)**; `tnum` cho số điểm.
- `ui/theme/Dimens.kt` — `RadiusButton` 12→**10**dp; thêm `GradientOverlayAlpha = 0.88f`.
- `res/values/colors.xml` — `animike_background` `#0B0E14`→**`#08080B`**.

**Đợt 2 — Components**
- `ui/components/AnimeCard.kt` — ScoreBadge chữ nâu; số rank dùng style mới; cân
  nhắc viền 1dp `outline` quanh poster.
- `ui/components/{ReviewCard, TopCharacterCard, NewEpisodeCard, CommunityRecommendationCard,
  ScrollToTopButton, Shimmer, BackButton, WatchStatusUi}.kt` — áp gradient/border/radius;
  `NewEpisodeCard` đổi `0.85`→`GradientOverlayAlpha`.

**Đợt 3** `ui/home/HomeScreen.kt` (+ section). **Đợt 4** `ui/detail/DetailScreen.kt`
(2 chỗ gradient), `ui/episodes/*`. **Đợt 5** `ui/search/SearchScreen.kt`,
`SearchFilterScreen.kt`. **Đợt 6** `ui/favorites/FavoritesScreen.kt`. **Đợt 7**
`ui/schedules/*`, `ui/seasonarchive/*`, `ui/browse/*`. **Đợt 8** `ui/characterdetail/*`
(gradient), `ui/persondetail/*`, `ui/studiodetail/*`, `ui/topcharacters/*`,
`ui/characters/*`, `ui/reviews/*`, `ui/reviewdetail/*`, `ui/newepisodes/*`,
`ui/communityrecommendations/*`.

**Đợt 9 — Light mode**
- `ui/theme/Color.kt` — thêm bộ hằng số light.
- `ui/theme/Theme.kt` — thêm `lightColorScheme`; **chuyển `success/warning/rank*`
  thành scheme-aware** (`LocalThemeMode` hoặc `isSystemInDarkTheme`);
  `AniMikeTheme(darkTheme, …)`.
- **mới** `ThemePreferences` + DataStore + `ui/settings/` (toggle Sáng/Tối/Theo hệ
  thống) + route trong `ui/navigation/`.
- **mới** `res/values-night/colors.xml`; `MainActivity` áp mode trước `setContent`.

**Đợt 10 — Icon vector**
- **mới** `res/drawable/ic_*.xml`. `ui/navigation/AniMikeNavHost.kt` + `Routes.kt` —
  bottom bar `Text(item.icon)` → `Icon(painterResource)`, active gold. Thay dần
  glyph `★/♥/…` ở các màn.

**Đợt 11 — Polish**: rà motion/hairline; **xoá font Inter** + hằng số thừa.

## Thứ tự thực thi

1. Bundle font → sửa `Type.kt` (compile để bắt lỗi thiếu weight).
2. `Color.kt` → `Theme.kt` (dark remap) → `Dimens.kt` → `colors.xml`. Build, chạy
   app, đối chiếu `docs/UI/`. Commit **Đợt 1**.
3. Đợt 2 → 3 → 4 → 5 → 6 → 7 → 8 (mỗi đợt: sửa → build → review → commit).
4. Đợt 9 Light mode (hạ tầng ThemeMode → màu scheme-aware → toggle → WCAG).
5. Đợt 10 Icon (thiết kế bộ icon → import drawable → thay bottom bar → thay glyph).
6. Đợt 11 Polish + dọn Inter.

## Rủi ro cần theo dõi

- **Tương phản chữ nâu `#2A1F08` trên gold `#D4AF6A`** (score badge/nút) — accessibility-reviewer
  xác nhận WCAG ở Đợt 1.
- **Cormorant serif chỉ dùng ≥20sp** (display/headline); mọi chữ <16sp giữ Montserrat.
- **Light WCAG** (đợt 9): nền cream + gold đậm `#9A6F26`; `rankGold` light `#B8892E`
  rất gần `primary` light — không đặt cạnh nhau.
- **Icon (đợt 10)**: khối lượng thiết kế lớn — bắt đầu bộ tối thiểu (4 tab bottom +
  favorite/back/star/search/filter/play) rồi mở rộng; stroke đồng bộ, touch ≥48dp.
- **Phạm vi**: mọi đợt CHỈ đổi da — không thêm section/endpoint, giữ giới hạn 3–4 tab.

## Gợi ý agent

- **Thực thi**: reskin màn → agent UI/compose (compose-ui-builder nếu tạo). Đợt 9
  phần DataStore → data engineer; Settings/Theme → UI. Đợt 10 thiết kế icon → skill
  `ui-ux-pro-max:design`. Đợt 11 → skill `compose-android` nếu jank.
- **Review**: mọi đợt chạy **compose-reviewer**; đợt đụng tương phản/ảnh/toggle
  (1,2,3,4,5,7,9,10) chạy thêm **accessibility-reviewer**; Đợt 9 lấy accessibility-reviewer
  làm gác chính.
