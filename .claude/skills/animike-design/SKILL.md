---
name: animike-design
description: >
  AniMike design system — dark anime-style theme. Use whenever building or modifying
  any UI/Compose screen, component, color, typography, or spacing in this project.
  All UI code MUST follow these tokens and rules. Also use when creating mockups
  or reviewing UI code for design consistency.
version: 1.0.0
---

# AniMike Design System

Phong cách: **dark anime-style** — nền tối, ảnh cover làm chủ đạo, accent neon. Tham khảo cảm giác của AniList/Crunchyroll nhưng có bản sắc riêng.

## Nguyên tắc chung

1. **Ảnh là nhân vật chính** — cover art chiếm diện tích lớn, UI lùi về sau (nền tối, chữ gọn)
2. **Dark-first** — thiết kế cho dark mode trước; light mode là biến thể phụ (có thể bỏ qua ở v1)
3. **Một accent duy nhất mỗi ngữ cảnh** — không dùng 2 màu neon cạnh nhau
4. **Mọi giá trị lấy từ token** — không hardcode màu/kích thước trong composable

## Design Tokens

### Màu (dark theme)

| Token | Hex | Dùng cho |
|---|---|---|
| `background` | `#0B0E14` | Nền màn hình |
| `surface` | `#151A23` | Card, bottom bar, dialog |
| `surfaceVariant` | `#1E2530` | Chip, search field, nền phụ |
| `primary` | `#8B5CF6` | Accent chính (tím) — nút, tab active, link |
| `secondary` | `#F471B5` | Accent phụ (hồng) — badge, favorite |
| `tertiary` | `#38BDF8` | Info (xanh cyan) — score, airing status |
| `onBackground` | `#E5E9F0` | Chữ chính |
| `onSurfaceVariant` | `#8B93A7` | Chữ phụ, caption, icon inactive |
| `error` | `#F87171` | Lỗi |
| `success` | `#4ADE80` | Trạng thái "Airing" |

Gradient overlay trên ảnh cover: `#0B0E14` alpha 0→85% (bottom-up) để chữ đè lên ảnh luôn đọc được.

### Typography

Font: **Inter** (Google Fonts) — fallback system default. Số liệu score dùng tabular figures.

| Style | Size / Weight | Dùng cho |
|---|---|---|
| `displaySmall` | 24sp / Bold | Tên anime ở Detail |
| `titleMedium` | 16sp / SemiBold | Section header ("Season Now", "Top Anime") |
| `bodyMedium` | 14sp / Regular | Synopsis, nội dung |
| `labelLarge` | 14sp / Medium | Nút, tab |
| `labelSmall` | 11sp / Medium | Chip genre, meta info (year, type, eps) |

Title anime trên card: tối đa 2 dòng, ellipsis.

### Spacing & Shape

- Spacing scale: `4 / 8 / 12 / 16 / 24 / 32` dp. Padding màn hình: **16dp**. Gap giữa card trong LazyRow: **12dp**
- Corner radius: card **12dp**, chip **8dp**, bottom sheet **20dp** (top), nút **12dp**
- Elevation: gần như không dùng shadow — phân tầng bằng màu surface

## Component rules

### AnimeCard (dọc — dùng trong LazyRow/Grid)
- Kích thước ảnh: tỉ lệ **2:3**, bo góc 12dp, rộng 120dp (row) hoặc fill (grid 3 cột)
- Dưới ảnh: title (2 dòng) + hàng meta: score (★ màu `tertiary`) · year
- Loading: shimmer placeholder màu `surfaceVariant`

### Detail header
- Ảnh cover full-width phía trên, gradient overlay, title + meta đè lên phần dưới ảnh
- Hàng chip genre cuộn ngang, chip nền `surfaceVariant`, chữ `onSurfaceVariant`
- Nút favorite: icon trái tim, active = `secondary`

### Score badge
- Nền `surface` alpha 80%, icon ★ + số 1 chữ số thập phân (VD "8.7"), chữ `tertiary`

### Trạng thái UI
- Loading: shimmer, KHÔNG dùng CircularProgressIndicator giữa màn hình trừ lần tải đầu
- Error: illustration/emoji nhẹ + message + nút "Thử lại" (primary)
- Empty: message thân thiện, không để màn trắng

### Bottom navigation
- 3-4 tab: Home, Search, Favorites (+ Seasons). Icon outline khi inactive (`onSurfaceVariant`), filled + `primary` khi active. Nền `surface`

## Mapping vào Compose

- Định nghĩa tokens trong `ui/theme/Color.kt`, map vào `darkColorScheme()` trong `Theme.kt`
- Composable chỉ dùng `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*` — cấm hardcode `Color(0xFF...)` ngoài file theme
- Spacing dùng object `Dimens` trong `ui/theme/Dimens.kt`
- Khi cần pattern chi tiết (atomic design, component structure), tham khảo skill `compose-expert` → `references/atomic-design.md` và `references/theming-material3.md`
