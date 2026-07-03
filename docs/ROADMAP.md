# AniMike — Roadmap & Quy trình phát triển

Hiện trạng project: Kotlin + Jetpack Compose (Material 3), minSdk 24, targetSdk 36 — mới chỉ là template trống.

**Skills & agents đã cài trong `.claude/`** (Claude Code tự nhận, xem `CLAUDE.md` ở root):
- `skills/compose-expert` — guide Compose chuyên sâu (24 references + source androidx)
- `skills/animike-design` — design system dark anime-style (bắt buộc cho mọi UI)
- `skills/jikan-api` — quy ước networking Jikan v4
- `agents/compose-reviewer` — agent review code sau mỗi feature

---

## 1. Kiến trúc đề xuất

**MVI + Repository**, chia package đơn giản (không cần multi-module ở giai đoạn này):

```
com.lambao.animike/
├── data/
│   ├── remote/        # Retrofit API interface, DTO (response model)
│   ├── local/         # Room: entity, dao, database
│   └── repository/    # Gộp remote + local, expose Flow
├── domain/            # Model dùng trong UI, mapper DTO → model
├── ui/
│   ├── home/          # HomeScreen, HomeViewModel, HomeContract (State/Event/Effect)
│   ├── search/
│   ├── detail/
│   ├── favorites/
│   ├── components/    # Composable dùng chung (AnimeCard, ErrorView...)
│   ├── navigation/    # NavHost, route definitions
│   └── theme/         # (đã có)
└── di/                # Hilt modules
```

**MVI contract** — mỗi màn hình gồm 3 phần trong file `XxxContract.kt`:
- `XxxState`: data class immutable, toàn bộ trạng thái UI (list, isLoading, error...)
- `XxxEvent`: sealed interface — mọi hành động của user (OnAnimeClick, OnRetry, OnQueryChange...)
- `XxxEffect`: sealed interface — hiệu ứng one-shot (NavigateToDetail, ShowSnackbar)

ViewModel expose `StateFlow<State>` + `Flow<Effect>`, nhận event qua 1 hàm `onEvent()`. Composable chỉ render state và gửi event — không chứa logic.

Nguyên tắc: **UI → (Event) → ViewModel → Repository → (API | Room) → (State) → UI**. UI không bao giờ gọi thẳng API.

Chi tiết pattern: xem skill `compose-expert` (`references/paging-mvi-testing.md`) và checklist trong `.claude/agents/compose-reviewer.md`.

## 2. Thư viện cần thêm

| Mục đích | Thư viện |
|---|---|
| Networking | Retrofit + OkHttp (logging interceptor) |
| JSON | kotlinx.serialization (hoặc Moshi) |
| DI | Hilt |
| Ảnh | Coil (coil-compose) |
| Navigation | Navigation Compose |
| Database | Room (+ ktx) |
| Phân trang | Paging 3 (paging-compose) |
| ViewModel | lifecycle-viewmodel-compose |

Khai báo trong `gradle/libs.versions.toml` rồi thêm vào `app/build.gradle.kts`. Cần thêm quyền `INTERNET` vào AndroidManifest.

## 3. Xử lý rate limit Jikan (quan trọng)

- **OkHttp Interceptor** giới hạn tốc độ: tối đa ~1 request / 400ms
- Retry với backoff khi gặp HTTP **429** (Too Many Requests) và **503**
- **Room làm cache**: dữ liệu đã tải (chi tiết anime, season) lưu DB, chỉ gọi lại API khi cache cũ hơn ~24h
- Trên Home, tải tuần tự từng section thay vì gọi 4 API cùng lúc

## 4. Roadmap theo phase

### Phase 0a — Design (1-2 ngày)
- [ ] Duyệt mockup các màn chính (Home, Detail, Search) theo design system `animike-design` — dark anime-style
- [ ] Implement theme: `Color.kt`, `Type.kt` (font Inter), `Dimens.kt` theo tokens trong `.claude/skills/animike-design/SKILL.md`

### Phase 0b — Nền tảng (1-2 ngày)
- [ ] Thêm dependencies (mục 2), quyền INTERNET
- [ ] Setup Hilt (Application class, module cung cấp Retrofit/Room)
- [ ] Tạo cấu trúc package như mục 1 + base MVI (BaseViewModel hoặc convention Contract)
- [ ] Định nghĩa `JikanApi` interface + DTO cho 1 endpoint (`/top/anime`), gọi thử hiển thị list đơn giản → xác nhận pipeline hoạt động

### Phase 1 — Home (3-5 ngày)
- [ ] HomeScreen: các section Season Now / Top / Upcoming (LazyColumn + LazyRow)
- [ ] AnimeCard component (ảnh Coil, title, score)
- [ ] Loading/error state + retry
- [ ] Navigation Compose: Home → Detail (truyền `malId`)

### Phase 2 — Detail + Search (5-7 ngày)
- [ ] DetailScreen: `/anime/{id}/full` — ảnh, synopsis, thông tin, trailer (mở YouTube), genres
- [ ] Tab/section: nhân vật, đề xuất, anime liên quan
- [ ] SearchScreen: TextField + debounce (~500ms), Paging 3 cho kết quả
- [ ] Bộ lọc: type, status, genre, sắp xếp

### Phase 3 — Favorites & offline (3-4 ngày)
- [ ] Room: entity + DAO cho favorites
- [ ] Nút yêu thích ở Detail, màn hình Favorites (bottom navigation)
- [ ] Cache chi tiết anime vào Room → xem offline

### Phase 4 — Hoàn thiện v1 (3-5 ngày)
- [ ] Lịch chiếu theo thứ (Schedules), Season Archive
- [ ] Rate limiter + retry 429 hoàn chỉnh
- [ ] Icon app, splash screen, review lại dark mode
- [ ] Test trên nhiều kích thước màn hình; release build (minify + ProGuard rules cho Retrofit/serialization)

### Phase 5 — v1.x / v2 (xem FEATURES.md mục 2-3)
- Local tracking tiến độ xem → Nhân vật/People → Reviews → Random/Watch → Manga → Notifications

## 5. Quy trình làm việc (cho người mới)

1. **Git**: commit nhỏ, thường xuyên. Làm feature trên branch riêng (`feature/home-screen`), xong merge vào `main`. `main` luôn build được.
2. **Mỗi tính năng làm theo vòng**: DTO/API → Repository → Contract (State/Event/Effect) → ViewModel → UI → test tay → chạy agent `compose-reviewer` → commit.
3. **Đừng làm nhiều tính năng cùng lúc** — xong hẳn 1 màn hình rồi mới sang màn tiếp theo.
4. **Test**: giai đoạn đầu ưu tiên test tay + unit test cho mapper/repository. Chưa cần UI test.
5. **Debug API**: bật OkHttp logging interceptor (chỉ ở debug build) để xem request/response trong Logcat.
6. **Phát hành** (khi sẵn sàng): tạo keystore ký app → build AAB → tài khoản Google Play Console ($25 một lần) → internal testing trước, production sau.

## 6. Tài liệu tham khảo

- Jikan v4 docs: https://docs.api.jikan.moe/
- Base URL: `https://api.jikan.moe/v4`
- Compose: https://developer.android.com/develop/ui/compose
- Paging 3: https://developer.android.com/topic/libraries/architecture/paging/v3-overview
