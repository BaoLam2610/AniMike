# AniMike

Ứng dụng Android xem thông tin anime (manga ở phase sau). Dữ liệu từ Jikan API v4 (https://api.jikan.moe/v4) — free, read-only, rate limit 3 req/s & 60 req/min.

## Stack

- Kotlin + Jetpack Compose (Material 3), minSdk 24, targetSdk 36
- Kiến trúc: **MVI** (State/Event/Effect) + Repository
- Package: `com.lambao.animike` — chia `data/` (remote, local, repository), `domain/`, `ui/` (feature packages), `di/`

## Tài liệu project — đọc trước khi làm việc

- `docs/FEATURES.md` — danh sách tính năng theo phase, map endpoint Jikan
- `docs/ROADMAP.md` — kiến trúc, thư viện, thứ tự triển khai

## Skills & Agents

- `.claude/skills/compose-expert/` — hướng dẫn Compose chuyên sâu (state, performance, navigation, paging, MVI). Tham khảo `references/` khi code UI
- `.claude/skills/animike-design/` — **design system bắt buộc** cho mọi UI: dark anime-style, tokens màu/typography/spacing
- `.claude/skills/jikan-api/` — quy ước networking: rate limit, DTO, error handling, caching
- `.claude/agents/compose-reviewer.md` — agent review code sau mỗi feature

## Quy tắc quan trọng

1. UI tuân thủ design system trong `animike-design` — không hardcode màu/kích thước
2. Networking tuân thủ `jikan-api` — luôn có rate limiter, retry 429, DTO nullable
3. MVI: composable chỉ render state + gửi event; logic nằm trong ViewModel/Repository
4. Mỗi feature xong: chạy build (`./gradlew assembleDebug`) và review trước khi commit
5. Commit nhỏ, message tiếng Anh, prefix: `feat:`, `fix:`, `refactor:`, `docs:`
