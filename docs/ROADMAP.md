# AniMike — Roadmap & Quy trình phát triển

Hiện trạng project: Kotlin + Jetpack Compose (Material 3), minSdk 24, targetSdk 36.
Đã hoàn thành Phase 0a → MVP 1 (Phase 3) + Season Archive — đang ở MVP 2 (Phase 4).

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

Mọi ViewModel kế thừa `ui/base/BaseViewModel<State, Event, Effect>` — lớp base giữ `_state`/`state`, `_effect`/`effect` (Channel) và các hàm `setState { }`/`sendEffect()`; subclass chỉ cần override `onEvent()`. Quyết định dùng base class ngay từ đầu (thay vì đợi 3 screen rồi mới rút) vì đây là phần lõi ổn định, ít khả năng cần hình dạng khác nhau giữa các screen — nếu một screen cần thêm flow riêng (VD Paging ở SearchScreen, xem `paging-mvi-testing.md`), cứ khai thêm `val items: Flow<PagingData<T>>` bên cạnh, không cần đưa vào base.

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

- **OkHttp Interceptor** giới hạn tốc độ: tối đa ~1 request / 400ms ✅ (Phase 0b)
- Retry với backoff khi gặp HTTP **429** (Too Many Requests) và **503** ✅ (Phase 0b)
- Trên Home, tải tuần tự từng section thay vì gọi 4 API cùng lúc ✅ (Phase 1, Mutex)
- **Room làm cache — stale-while-revalidate** (Phase 3): UI luôn đọc từ Room qua
  `Flow`; emit cache ngay, hết TTL thì refresh nền rồi ghi đè theo key → Flow tự
  re-emit; pull-to-refresh bỏ qua TTL. TTL: genres 7 ngày, list/detail 24h,
  search không cache. Jikan không hỗ trợ HTTP cache/ETag (đã verify) nên đây là
  tầng cache duy nhất — chi tiết trong `.claude/skills/jikan-api/SKILL.md` mục Caching

## 3b. [Chưa làm] Cache cho dữ liệu phụ ở Detail (Characters/Episodes/Recommendations/Reviews)

Hiện tại 4 mục này (nhân vật, tập, đề xuất, đánh giá) ở màn Detail **không cache
Room** — mỗi lần mở Detail đều gọi lại API (xem comment ở
`AnimeDetailRepository.kt`, lý do ban đầu: "refetch rẻ hơn thêm entity/DAO").
Việc này ổn cho MVP nhưng tốn request lặp lại không cần thiết khi user vào ra
Detail nhiều lần. Ý tưởng khi làm (ưu tiên thấp, không chặn MVP3):

- Áp SWR (giống Home/Detail chính) cho cả 4 mục: entity riêng theo `malId`
  (+ `fetchedAt`), Flow từ Room hiện ngay cache cũ, refresh nền theo TTL.
- **"Các tập" chỉ cache đúng phần preview (10 tập đầu)** — khớp với những gì
  user thực sự thấy ở Detail, không cache cả list đầy đủ của EpisodesScreen
  (màn đó đã tự paginate qua Paging 3, không cần Room).
- **TTL nên khác nhau theo bản chất dữ liệu, không dùng 1 con số chung**:
  - Characters, Recommendations: gần như tĩnh (nhân vật/đề xuất của 1 anime
    hiếm khi đổi) → TTL dài, ví dụ 7 ngày như genres.
  - Reviews: user MAL đăng review liên tục bất kể anime đang chiếu hay đã
    xong → TTL ngắn hơn, ví dụ 24h giống list thường.
  - **Episodes: TTL nên phụ thuộc `AnimeDetail.isAiring`** (field đã có sẵn,
    không cần gọi thêm API để biết) — anime đang chiếu (`isAiring = true`) có
    thể ra tập mới bất cứ lúc nào → TTL ngắn (vài giờ); anime đã kết thúc
    (`isAiring = false`) thì danh sách tập gần như đóng băng vĩnh viễn → TTL
    dài (7+ ngày, hoặc coi như never-expire vì user vẫn có thể force-refresh
    thủ công nếu nghi ngờ). Đây là câu trả lời cho vấn đề "không rõ lịch cập
    nhật của Jikan" — không cần đoán lịch của Jikan, chỉ cần dùng tín hiệu
    airing status mình đã có sẵn cục bộ để quyết định độ tin cậy của cache.

## 4. Roadmap theo phase

Nguyên tắc: mỗi MVP là một đợt nhỏ, **xong hẳn (build + review + commit) mới sang đợt sau** — không phát triển nhiều thứ cùng lúc.

### Phase 0a — Design ✅ (2026-07)
- [x] Duyệt mockup các màn chính (Home, Detail, Search) theo design system `animike-design` — bản duyệt nằm ở Artifact (Figma MCP bị giới hạn quota)
- [x] Implement theme: `Color.kt`, `Type.kt` (font Inter bundle local), `Dimens.kt` theo tokens

### Phase 0b — Nền tảng ✅ (2026-07)
- [x] Dependencies + quyền INTERNET (lưu ý trần version: xem `gradle/libs.versions.toml` — AGP 9.0.1/compileSdk 36 cần lifecycle ≤2.10.0, androidx.hilt ≤1.3.0, 2 cờ trong `gradle.properties`)
- [x] Setup Hilt (Application class, NetworkModule, RepositoryModule)
- [x] Cấu trúc package như mục 1 + base MVI: `ui/base/BaseViewModel.kt`
- [x] `JikanApi` + DTO + rate-limit/retry interceptor, pipeline `/top/anime` → list — verified trên thiết bị thật

### Phase 1 — Home ✅ (2026-07)
- [x] HomeScreen: 3 section Season Now / Top / Upcoming (LazyColumn + LazyRow), tải tuần tự với Mutex
- [x] AnimeCard component (Coil, 2:3, score badge) + shimmer loading
- [x] Loading/error/empty state + retry độc lập từng section
- [x] Navigation Compose: Home → Detail (truyền `malId`)

### Phase 2 — Detail + Search ✅ (2026-07)
- [x] DetailScreen: `/anime/{id}/full` — hero header + gradient, synopsis, trailer (YouTube), genres
- [x] Section: nhân vật + seiyuu, đề xuất (điều hướng Detail→Detail), anime liên quan
- [x] SearchScreen: TextField + debounce 500ms + nút xóa, Paging 3, danh sách duyệt mặc định khi chưa gõ
- [x] Bộ lọc: type, status, genre (multi-select), sắp xếp

### MVP 1 (Phase 3) — Favorites & offline ✅ (2026-07)
- [x] Room: entity + DAO cho favorites
- [x] Nút yêu thích ở Detail, màn hình Favorites
- [x] Bottom navigation: Home / Search / Duyệt (Season Archive) / Favorites
- [x] Cache stale-while-revalidate cho Home sections + Detail (xem mục 3):
  entity list dạng `(listKey, malId, ..., position, fetchedAt)` cho season/top/upcoming,
  entity detail theo `malId`, genres TTL 7 ngày; repository chuyển sang expose `Flow` từ Room
- [x] Pull-to-refresh trên Home (bỏ qua TTL)
- [x] Season Archive (`/seasons/{year}/{season}`, `/seasons`) — làm sớm hơn kế hoạch, đã qua compose-reviewer

### MVP 2 (Phase 4) — Chốt v1 ✅ phần tính năng (2026-07)
- [x] Lịch chiếu theo thứ (`/schedules`) — gộp chung tab "Duyệt" với Season Archive qua
  segmented control (giữ đúng giới hạn 3-4 tab của `animike-design` SKILL.md), mặc định
  chọn đúng thứ hôm nay
- Icon app / splash / release build: **hoãn** — dự án cá nhân chưa cần publish, dồn
  splash vào MVP 3 UI-1, release build để khi nào thực sự cần cài lâu dài

### MVP 3 — Nâng cấp UI theo kit Animax ← ĐANG LÀM
Ảnh tham chiếu đã export tại `docs/UI/` (27 màn, đặt tên `{số}_{Dark|Light}_{tên màn}.png`)
— **không cần mockup riêng nữa, ảnh kit chính là mockup**. Đánh giá khả thi so với Jikan:
làm giống được ~80%; không làm được Play/Download video (Jikan chỉ có metadata — xem
FEATURES.md mục 4), Profile không có account nhưng làm được dạng Settings + thống kê local.

Triển khai lần lượt từng đợt, mỗi đợt: code → build → compose-reviewer → user commit:
- [x] **UI-1: Tokens + AnimeCard + Splash** ✅ — giữ accent tím (không đổi theo kit),
  score badge góc trên-trái card (nền primary, chữ onPrimary), splash logo chevron "A"
  + core-splashscreen. Cập nhật `animike-design` SKILL.md khớp quyết định.
- [x] **UI-2: Home** ✅ — hero header full-bleed từ Season Now[0] + gradient + nút
  "Xem chi tiết"/"Yêu thích" (thay Play — Jikan không có video), "Top Hits Anime" có số
  thứ hạng đè card. Bỏ hẳn hàng "Season Now" riêng (trùng với hero + đã có ở tab Duyệt).
- [x] **UI-3: Release Calendar** ✅ — gộp chung tab "Duyệt" với Season Archive (segmented
  control, giữ đúng giới hạn tab), chip ngày kèm ngày dương lịch, list dọc thumbnail +
  giờ chiếu, mặc định chọn đúng thứ hôm nay. (Bỏ qua vạch "giờ hiện tại" — quá phức tạp
  so với giá trị mang lại cho quy mô dự án cá nhân)
- [x] **UI-4: Detail** ✅ — hero + meta chips, nút Trailer, back/favorite neo cố định,
  section Episodes (đổi sang `/videos/episodes` có thumbnail + mới→cũ, Paging 3 "Xem tất
  cả"), Nhân vật (preview 15 + "Xem tất cả" có search local), tab Đề xuất/Đánh giá
  (Reviews cũng Paging 3 "Xem tất cả"), animation cho section pop-in + expand/collapse.
  Đã vượt xa scope UI ban đầu (thêm 3 màn con + Paging), coi như đã bao trọn phần data
  layer còn thiếu của Detail luôn.
- [x] **UI-5: My List** ✅ — Favorites → grid 2 cột poster lớn (AnimeCard thêm
  `showTitle=false`) + score badge, đổi tên tab "Yêu thích"→"Danh sách" + icon
  bookmark khớp kit (không phải trái tim, glyph đó vẫn dùng cho hành động
  favorite ở Home/Detail). Ref: `44/45_Dark_my list`
- [x] **UI-6: Search + Sort & Filter** ✅ — nút filter mở màn "Sắp xếp & Lọc"
  full-screen riêng (share chung SearchViewModel qua NavBackStackEntry, draft
  local + Reset/Apply) thay 2 hàng chip cũ. Sort thêm "Mới nhất" (order_by=
  start_date), Year filter map sang start_date/end_date (Jikan không có tham
  số year riêng). Bỏ qua "Region" (All/Japan/Chinese/Others) trong kit — Jikan
  /anime search không hỗ trợ lọc theo quốc gia. Giữ chip solid-bg theo design
  system hiện có, không đổi sang outline-chip như kit. Ref: `25-29_Dark_search`,
  `28_Dark_sort & filter`
- [x] **Phát sinh: Gallery ảnh trong Detail** ✅ — section "Hình ảnh"
  (`/anime/{id}/pictures`, FEATURES.md mục 1.3, không có mockup trong kit):
  LazyRow poster 2:3 đặt giữa "Liên quan" và tab Đề xuất/Đánh giá, bấm ảnh mở
  viewer full-screen (Dialog + HorizontalPager, đếm trang). Lưu ý: endpoint
  này CHỈ có ảnh — trailer đã nằm sẵn trong `/full` (nút "Xem trailer" có từ
  UI-4), không có link trailer riêng ở /pictures.
- [x] **Phát sinh: Trailer card trong Detail** ✅ — thay nút text "Xem trailer"
  bằng card thumbnail 16:9 + play overlay (vị trí nút "Play" pill của kit;
  thumbnail derive từ `img.youtube.com/vi/{id}/hqdefault.jpg`, không đổi
  schema Room). Kèm fix data: `trailer.youtube_id` đôi khi null dù trailer
  tồn tại (VD anime 38524 chỉ có `embed_url`) — mapper rút id từ
  `embed_url`/`url` làm fallback.
- [ ] Polish motion/transition giữa các màn hình (sau cùng, khi các màn đã ổn định)

Tính năng mở rộng Detail (từng nằm ở MVP 3 cũ, chuyển sang MVP 4 vì thuộc nhóm dữ liệu mới):
nút "Xem trên..." (`/anime/{id}/streaming`), tab media (`/anime/{id}/videos`)

### MVP 4 — Khám phá
- [ ] Random anime "Hôm nay xem gì?" (`/random/anime`)
- [ ] Tập mới phát hành + promo mới (`/watch/episodes`, `/watch/promos`) — khớp section
  "New Episode Releases" trên Home của kit Animax
- [ ] Đề xuất cộng đồng (`/recommendations/anime`)
- [ ] Biểu đồ phân bố điểm + số người xem (`/anime/{id}/statistics`), nhạc OP/ED (`/anime/{id}/themes`)
- [ ] Nút "Xem trên..." (`/anime/{id}/streaming`), tab media (`/anime/{id}/videos`) — chuyển từ MVP 3 cũ

### MVP 5 — Nhân vật / Người / Studio
- [ ] Top nhân vật (`/top/characters`), trang nhân vật (`/characters/{id}/full`)
- [ ] Trang seiyuu/staff (`/people/{id}/full`, `/anime/{id}/staff`)
- [ ] Trang studio (`/producers/{id}/full`) — bấm tên studio ở Detail để mở

### MVP 6 — Tracking local
- [ ] Trạng thái xem: Đang xem / Đã xem / Tạm dừng / Bỏ / Dự định xem (Room)
- [ ] Đánh dấu tập đã xem, chấm điểm cá nhân

### Xa hơn (xem FEATURES.md mục 2-3)
- Reviews → Manga → Xem profile MAL công khai → Notification lịch chiếu → Widget/Deep link

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
