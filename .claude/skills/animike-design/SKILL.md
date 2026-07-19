---
name: animike-design
description: >
  AniMike design system — premium cinema-dark theme, champagne-gold accent. Use
  whenever building or modifying any UI/Compose screen, component, color,
  typography, or spacing in this project. All UI code MUST follow these tokens
  and rules. Also use when creating mockups or reviewing UI code for design
  consistency.
version: 2.0.0
---

# AniMike Design System

Phong cách: **premium cinema-dark** — nền OLED gần đen, ảnh poster làm chủ đạo,
accent **champagne gold** tinh tế, typography serif cho tiêu đề. Cảm giác cao cấp,
điện ảnh (tham khảo Netflix/iQIYI về bố cục, nhưng bản sắc riêng: sang trọng thay
vì rực rỡ). Chuyển động tiết chế (motion 3/10) — mượt, không phô trương.

## Lịch sử hướng thị giác

- **v1.0 (MVP 0–6, 2026-07):** dark anime-style, accent tím `#8B5CF6`, font Inter.
  Layout học theo **kit Animax** (ảnh export tại `docs/UI/`).
- **v2.0 (redesign 2026-07):** đổi hẳn hướng sang **premium/luxury** theo yêu cầu
  user — accent **champagne gold `#D4AF6A`** (khác biệt với Netflix đỏ / iQIYI xanh
  lá / bản tím cũ), typography **Cormorant + Montserrat**, chuẩn bị **Light mode**
  + đa ngôn ngữ + switch theme, và **icon vector thiết kế riêng**. Quyết định của
  user: mood "sang trọng cao cấp", variance 7/10, motion 3/10, cảm hứng
  Netflix/iQIYI/Animax kit. **Vẫn giữ nguyên bố cục/layout theo kit Animax**
  (`docs/UI/`) — chỉ đổi bảng màu / font / độ bo / accent. Khi kit và tokens ở
  đây lệch nhau về màu → tokens ở đây thắng; về layout → kit thắng.

> ⚠️ Đây là **spec** để rollout. Cập nhật file này KHÔNG tự đổi app — code thật
> (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Dimens.kt`) phải được sửa theo
> từng đợt (dùng agent `feature-planner` để chia đợt như MVP3).

## Nguyên tắc chung

1. **Ảnh là nhân vật chính** — poster art chiếm diện tích lớn, UI lùi về sau
   (nền tối trung tính, chữ gọn, accent gold dùng dè)
2. **Dark-first, light-ready** — thiết kế cho dark trước; light mode là biến thể
   đầy đủ (đã định token bên dưới), bật ở đợt rollout riêng
3. **Một accent duy nhất mỗi ngữ cảnh** — gold là accent chính; KHÔNG đặt 2 màu
   accent cạnh nhau. Sang trọng = tiết chế, không neon
4. **Mọi giá trị lấy từ token** — không hardcode màu/kích thước trong composable
5. **Chi tiết tinh tế** — hairline border thay đổ bóng nặng; bo góc vừa phải;
   khoảng trắng rộng (variance 7 nhưng vẫn dễ đọc)

## Design Tokens — Màu (Dark, mặc định)

| Token | Hex | Dùng cho |
|---|---|---|
| `background` | `#08080B` | Nền màn hình (OLED near-black) |
| `surface` | `#111013` | Card, bottom bar, dialog |
| `surfaceVariant` | `#1B1A1F` | Chip, search field, nền phụ |
| `outline` | `#2A2830` | Hairline border card/divider (đặc trưng luxury) |
| `primary` | `#D4AF6A` | Accent chính (champagne gold) — nút, tab active, link, score badge card |
| `onPrimary` | `#2A1F08` | Chữ/icon trên nền gold (nâu đậm, KHÔNG dùng đen thuần) |
| `secondary` | `#E0A9B0` | Accent phụ (rose gold) — favorite, badge |
| `onSecondary` | `#35171C` | Chữ trên nền rose gold |
| `tertiary` | `#8FB0C4` | Info (dusty blue) — airing status, score badge ★ ở Detail hero (không dùng cho AnimeCard) |
| `onBackground` | `#ECE6DA` | Chữ chính (warm off-white) |
| `onSurfaceVariant` | `#9A9184` | Chữ phụ, caption, icon inactive (warm muted) |
| `error` | `#D9605C` | Lỗi |
| `success` | `#6FBF8E` | Trạng thái "Airing" (muted jade) |

**Màu ngữ nghĩa mở rộng** (đã dùng trong code — giữ nguyên vai trò):

| Token | Hex | Dùng cho |
|---|---|---|
| `warning` | `#E3C06A` | Trạng thái "Tạm dừng" (ON_HOLD) — alias của `rankGold` |
| `rankGold` | `#E3C06A` | Huy hiệu #1 (Top nhân vật…) |
| `rankSilver` | `#C7CDD6` | Huy hiệu #2 |
| `rankBronze` | `#C68B5A` | Huy hiệu #3 |

> Lưu ý: primary (gold `#D4AF6A`) và rankGold (`#E3C06A`) gần nhau nhưng rankGold
> sáng hơn để phân biệt trên ribbon. Không đặt 2 màu này cạnh nhau trong cùng 1
> thành phần.

Gradient overlay trên ảnh cover: `#08080B` alpha **0→88%** (bottom-up) để chữ đè
lên ảnh luôn đọc được (đậm hơn v1 vì nền tối hơn).

## Design Tokens — Màu (Light, biến thể)

Bật ở đợt rollout riêng. Nền **cream ấm** (không trắng thuần) giữ chất luxury.
Gold phải **đậm hơn** để đạt tương phản WCAG trên nền sáng.

| Token | Hex | Ghi chú |
|---|---|---|
| `background` | `#FAF6EE` | Cream ấm |
| `surface` | `#FFFFFF` | Card |
| `surfaceVariant` | `#F1EBDF` | Chip, field |
| `outline` | `#E0D8C8` | Hairline border |
| `primary` | `#9A6F26` | Gold đậm (WCAG ≥4.5:1 với onPrimary) |
| `onPrimary` | `#FFFFFF` | |
| `secondary` | `#B26E77` | Rose đậm |
| `onSecondary` | `#FFFFFF` | |
| `tertiary` | `#3E6B85` | Dusty blue đậm |
| `onBackground` | `#1B1712` | Chữ chính |
| `onSurfaceVariant` | `#6C6456` | Chữ phụ |
| `error` | `#C0403C` | |
| `success` | `#2E8B5D` | |

## Typography

Fonts: **Cormorant** (serif — heading/display, cảm giác sang) + **Montserrat**
(sans — body/label/nút). Bundle local như Inter trước đây (minSdk 24, không phụ
thuộc mạng). Số liệu score dùng **tabular figures** (Montserrat `tnum`).

| Style | Size / Font-Weight | Dùng cho |
|---|---|---|
| `displaySmall` | 26sp / **Cormorant** SemiBold | Tên anime ở Detail hero |
| `headlineSmall` | 20sp / **Cormorant** Medium | Tiêu đề lớn (tùy chọn, VD tên section nổi bật) |
| `titleMedium` | 16sp / **Montserrat** SemiBold | Section header ("Top Hits Anime"…) |
| `bodyMedium` | 14sp / **Montserrat** Regular | Synopsis, nội dung |
| `labelLarge` | 14sp / **Montserrat** Medium | Nút, tab |
| `labelSmall` | 11sp / **Montserrat** Medium | Chip genre, meta (year, type, eps) |

Title anime trên card: tối đa 2 dòng, ellipsis. **Không** dùng Cormorant cho chữ
nhỏ < 16sp (serif khó đọc ở size nhỏ) — chỉ dành cho display/headline.

## Spacing & Shape

- Spacing scale: `4 / 8 / 12 / 16 / 24 / 32` dp. Padding màn hình **16dp**. Gap
  card trong LazyRow **12dp**. Luxury = khoáng đạt: ưu tiên 16/24 cho khối lớn.
- Corner radius: card **12dp**, chip **8dp**, nút **10dp**, bottom sheet **20dp** (top)
- **Hairline border**: card/khối nổi có thể thêm viền 1dp màu `outline` thay cho
  đổ bóng — tạo cảm giác "khung tranh" cao cấp. Elevation/shadow dùng rất hạn chế,
  phân tầng chủ yếu bằng màu surface + border.

## Component rules

### AnimeCard (dọc — dùng trong LazyRow/Grid)
- Kích thước ảnh: tỉ lệ **2:3**, bo góc 12dp, rộng 120dp (row) hoặc fill (grid 3 cột)
- Score badge đè **góc trên-trái ảnh** (padding 8dp) — xem mục Score badge
- Dưới ảnh: title (2 dòng, `titleMedium`/`bodyMedium`) + year (`labelSmall`, `onSurfaceVariant`)
- Loading: shimmer placeholder màu `surfaceVariant`
- Tùy chọn: viền 1dp `outline` quanh ảnh cho cảm giác framed

### Detail header
- Ảnh cover full-width phía trên, gradient overlay (0→88%), title (`displaySmall`
  Cormorant) + meta đè lên phần dưới ảnh
- Hàng chip genre cuộn ngang, chip nền `surfaceVariant`, chữ `onSurfaceVariant`
- Nút favorite: icon trái tim, active = `secondary` (rose gold)

### Score badge
Có 2 style tùy ngữ cảnh — **cả hai đều ẩn hẳn khi không có score** (mapper trả
`"N/A"`), không hiện "N/A" ở bất kỳ đâu:
- **Trên AnimeCard**: nền `primary` (gold), bo góc 8dp, chữ `onPrimary` (nâu đậm)
  `labelSmall`, số 1 chữ số thập phân (VD "8.7"), đè góc trên-trái ảnh cover
- **Trên Detail hero header**: nền `surface` alpha 80% (đè lên ảnh cover), icon ★
  + chữ `tertiary` (dusty blue) `labelLarge` — khác AnimeCard vì là badge nổi trên
  ảnh lớn cùng hàng pill "Airing"

### Trạng thái UI
- Loading: shimmer, KHÔNG dùng CircularProgressIndicator giữa màn hình trừ lần tải đầu
- Error: illustration/icon vector nhẹ + message + nút "Thử lại" (primary)
- Empty: message thân thiện, không để màn trắng

### Bottom navigation
- 3-4 tab: Home, Search, Duyệt, Danh sách. Icon **vector outline** riêng khi
  inactive (`onSurfaceVariant`), filled + `primary` (gold) khi active. Nền `surface`

### Icon (thiết kế riêng — v2)
- Bộ icon **vector SVG thiết kế riêng** (không dùng emoji làm icon; hạn chế phụ
  thuộc `Icons.Default.*` mặc định cho các icon mang tính thương hiệu)
- Style: outline, stroke đều, bo đầu mềm; đồng bộ 1 stroke-width. Kích thước
  chuẩn 24dp, touch target ≥48dp
- Tạo qua skill `ui-ux-pro-max:design` (icon design) → export SVG → đưa vào
  `res/drawable` dạng vector drawable

### Splash
- `androidx.core:core-splashscreen` (minSdk 24): nền `background`, logo màu
  `primary` (gold), không text/loader — hệ thống tự dismiss

## Mapping vào Compose

- Định nghĩa tokens trong `ui/theme/Color.kt`; map vào **`darkColorScheme()`** và
  **`lightColorScheme()`** trong `Theme.kt` (v2 hỗ trợ cả 2). `outline` map vào
  `ColorScheme.outline`; các màu ngữ nghĩa mở rộng (warning/rank*) khai qua
  CompositionLocal hoặc extension như hiện có
- Font Cormorant/Montserrat khai trong `ui/theme/Type.kt` (FontFamily bundle local)
- Composable chỉ dùng `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*`
  — cấm hardcode `Color(0xFF...)` ngoài file theme
- Spacing dùng object `Dimens` trong `ui/theme/Dimens.kt`
- Chi tiết pattern: skill `compose-expert` → `references/theming-material3.md`
  (đổi ColorScheme + light/dark), `references/atomic-design.md`,
  `references/animation.md` (motion nhẹ 3/10)
