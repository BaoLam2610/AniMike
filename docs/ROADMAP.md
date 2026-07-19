# AniMike — Roadmap & Quy trình phát triển

Hiện trạng project: Kotlin + Jetpack Compose (Material 3), minSdk 24, targetSdk 36.
Đã hoàn thành Phase 0a → MVP 6 (Tracking local) — tiếp theo xem mục "Xa hơn".

> **Redesign v2 (2026-07):** đang có kế hoạch đổi hẳn hướng thị giác sang premium
> cinema-dark + accent champagne gold — chi tiết 11 đợt ở `docs/REDESIGN_V2.md`
> (spec: `animike-design` SKILL.md v2.0.0 + `design/tokens.json`).

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

## 3b. Cache cho dữ liệu phụ ở Detail (Characters/Episodes/Recommendations/Reviews/Pictures)

- [x] **Episodes: KHÔNG cache, luôn gọi lại** ✅ — quyết định rõ ràng của
  user: tập mới có thể ra bất cứ lúc nào, cache (dù có TTL ngắn) vẫn có nguy
  cơ hiện thiếu tập mới ngay lúc mở Detail — trải nghiệm tệ hơn cái giá phải
  trả (1 request `/videos/episodes` mỗi lần vào màn). `getEpisodes()` vẫn
  one-shot như trước, không đổi. (Lưu ý: session này ban đầu từng thử cache
  Episodes + TTL theo `isAiring` — đã revert hoàn toàn vì hiểu sai yêu cầu.)
- [x] **Recommendations/Pictures/Reviews(preview)/Characters: cache Room + SWR** ✅ —
  ngược lại với Episodes, 4 mục này KHÔNG cần tươi theo từng phút/giờ nên
  đáng cache để tránh gọi lại API mỗi lần user vào/ra Detail:
  - **Recommendations**: tái dùng bảng `cached_anime_list`/`AnimeListDao` sẵn
    có (key `detail_recommendations_{malId}`, xem `AnimeListKey`) thay vì
    thêm entity/DAO riêng — shape `Anime` đã khớp. TTL 7 ngày
    (`CacheTtl.RECOMMENDATIONS_MS`) vì đề xuất của 1 anime hiếm khi đổi.
  - **Pictures**: bảng mới `cached_picture` (`malId`, `url`, `position`,
    `fetchedAt`). TTL 7 ngày (`CacheTtl.PICTURES_MS`) — poster art gần như tĩnh.
  - **Reviews (preview)**: bảng mới `cached_review_preview` (`malId`,
    `reviewId`, `username`, `score`, `reviewText`, `position`, `fetchedAt`),
    vẫn cắt còn `REVIEWS_LIMIT` (5) trước khi cache. TTL 24h
    (`CacheTtl.REVIEWS_PREVIEW_MS`, ngắn hơn các mục còn lại) vì user MAL đăng
    review liên tục. KHÔNG ảnh hưởng `reviewsPagingSource` (Paging 3 riêng
    cho ReviewsScreen "Xem tất cả").
  - **Characters**: bảng mới `cached_character` (`malId`, `characterId`,
    `name`, `imageUrl`, `role`, `voiceActorName`, `position`, `fetchedAt`) —
    thay cho cache tạm trong bộ nhớ (5 phút) ban đầu. TTL 7 ngày
    (`CacheTtl.CHARACTERS_MS`, giống Recommendations/Pictures) vì dàn nhân
    vật gần như tĩnh. Detail preview và `CharactersScreen` ("Xem tất cả") giờ
    cùng đọc 1 bảng qua `observeCharacters()`/`refreshCharacters()`, thay cho
    `getCharacters()` one-shot cũ.
  - Cả 4 dùng sentinel row (id âm/url rỗng) khi API trả rỗng thật, để
    `getFetchedAt` (MIN aggregate) không trả `null` mãi mãi và gây cache-miss
    vĩnh viễn — lọc bỏ sentinel khi đọc ra domain model.
  - Pull-to-refresh ở Detail (`DetailEvent.OnPullToRefresh`) force-refresh cả
    `/full` lẫn 4 cache này (bỏ qua TTL); Episodes không cần "force" vì vốn
    đã luôn gọi lại. DB version 4→5 (thêm `cached_picture`/`cached_review_preview`),
    6→7 (thêm `cached_character`, bỏ cache tạm trong bộ nhớ của Characters) —
    destructive migration, chấp nhận được vì chưa có user thật.

- [ ] **[Chưa làm] Dọn cache phình theo thời gian** — `cached_anime_list`
  (bucket `detail_recommendations_{malId}`), `cached_picture`,
  `cached_review_preview`, `cached_character` mỗi bảng tích lũy VĨNH VIỄN 1
  bucket riêng cho MỖI anime user từng mở Detail (khác 3 key cố định của Home
  luôn ghi đè tại chỗ) — không có cơ chế dọn dẹp. Không chặn vì kích thước
  mỗi bucket rất nhỏ (vài chục row), nhưng nên có ý tưởng dọn khi làm tiếp: ví
  dụ purge bucket cũ hơn N×TTL lúc app khởi động, hoặc giới hạn tổng số
  bucket kiểu LRU.

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

### MVP 3 — Nâng cấp UI theo kit Animax ✅ (2026-07)
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
- [x] **Phát sinh: Phát trailer trong app (thử, đã revert)** — thử nhúng
  WebView phát YouTube embed player ngay trong app (2 vòng: dialog full-screen
  → inline trong màn Detail, kèm fix lỗi "màn hình trắng" bằng
  `loadDataWithBaseURL` bọc `<iframe>`). **Revert lại điều hướng sang app
  YouTube** vì YouTube đã siết chặn embed player diện rộng phía họ (lỗi
  "Error 153", không phải bug riêng của app này, không có cách khắc phục từ
  phía client) — xem thảo luận: reddit.com/r/ObsidianMD/comments/1ogzv1s.
  `TrailerCard` giờ chỉ còn là thumbnail 16:9 + play overlay mở
  `youtube.com/watch?v={id}` qua Intent (như bản đầu tiên), không còn
  WebView/Dialog. Cũng sửa contrast label "Xem trailer" (gradient đáy 0→85%
  thay scrim phẳng 30% — scrim cũ không đủ tối khi thumbnail nền sáng).
  Khôi phục đúng kiến trúc MVI ban đầu: `TrailerCard` chỉ gửi
  `DetailEvent.OnTrailerClick(youtubeId)`, `startActivity()` thật sự nằm ở
  `DetailEffect.OpenYoutube` xử lý trong `LaunchedEffect` của DetailScreen
  (như 4 effect điều hướng khác) — bản revert đầu tiên lỡ gọi `startActivity`
  thẳng trong composable, bị compose-reviewer bắt lỗi và sửa lại.
- [x] **Polish motion/transition giữa các màn hình** ✅ — định nghĩa transition
  DÙNG CHUNG ở cấp `NavHost` (`enterTransition`/`exitTransition`/
  `popEnterTransition`/`popExitTransition`), không gán cứng theo từng
  `composable()`: xét CẶP route thực tế (`initialState`/`targetState`) để
  quyết định — đổi tab bottom-nav (cả 2 đầu đều là 1 trong 4 route gốc) →
  crossfade 200ms; mọi điều hướng còn lại (push/pop) → slide + fade 300ms.
  Cách này xử lý đúng route "lưỡng tính" như SEARCH (vừa là tab gốc vừa là
  điểm push sang SearchFilter) mà không cần override riêng từng composable.

Tính năng mở rộng Detail (từng nằm ở MVP 3 cũ, chuyển sang MVP 4 vì thuộc nhóm dữ liệu mới):
nút "Xem trên..." (`/anime/{id}/streaming`), tab media (`/anime/{id}/videos`)

### MVP 4 — Khám phá ✅ (2026-07)
- [x] **Random anime "Hôm nay xem gì?"** ✅ — card ngang trong Home (giữa hero
  và "Top Hits Anime", không có mockup nên tự thiết kế theo token
  animike-design): icon dice, bấm vào gọi `/random/anime` rồi điều hướng
  thẳng sang Detail của anime trả về. Không cache theo TTL (mỗi lần bấm phải
  thực sự ngẫu nhiên), nhưng CÓ ghi thẳng kết quả vào cache Detail
  (`AnimeDetailDao.upsert`) trước khi điều hướng — tránh Detail cache-miss
  gọi lại y hệt `/anime/{id}/full` lần nữa (phát hiện qua review, sửa ngay).
  Chặn double-tap bằng `isLoadingRandom` + đổi icon dice thành spinner trong
  lúc chờ; lỗi request thật hiện inline (tái dùng `SectionError` có sẵn) thay
  vì chỉ log im lặng như bản đầu (cũng phát hiện qua review, sửa ngay).
- [x] **Tập mới phát hành** ✅ — section "Tập mới phát hành" trên Home (khớp
  kit "New Episode Releases"), SWR + Room cache riêng (`cached_new_episode`,
  TTL 24h giống 3 list Home khác), preview 10 + "Xem tất cả" mở
  `NewEpisodesScreen` (grid 2 cột, KHÔNG Paging vì endpoint không hỗ trợ phân
  trang thật — đã verify qua curl: `page=1`/`page=2` trả về y hệt nhau, luôn 1
  snapshot cố định). Dùng `/watch/episodes/popular` thay vì `/watch/episodes`
  (bản thường) — verify qua curl cùng model response (entry/episodes/
  region_locked, KHÔNG cần đổi DTO/mapper) nhưng trả các bộ nổi tiếng hơn
  (Cowboy Bebop, Naruto...) thay vì phim bất kỳ; đã cân nhắc `/watch/promos/
  popular` nhưng response shape khác hẳn (có `trailer` object + `title` promo
  riêng, không có mảng `episodes`) nên không phù hợp để thay thế section này.
  Bỏ score badge của kit — endpoint này không có field score (không phải
  thiếu ngẫu nhiên, cấu trúc response cố định không trả). **Bỏ qua
  `/watch/promos`** (trailer/PV mới) — không có mockup riêng, và nội dung
  (danh sách trailer) trùng lặp về ý nghĩa với trailer card đã có sẵn trong
  Detail; để dành nếu sau này cần 1 tab "khám phá trailer" riêng.
  `NewEpisodeCard` gán `contentDescription` = tên anime + nhãn tập trên ảnh
  (không chỉ hiện "Episode N" — nhiều bìa không có logo/tên đọc được như ảnh
  mẫu trong kit) — phát hiện qua review, sửa ngay.
- [x] **Đề xuất cộng đồng** ✅ — section "Đề xuất cộng đồng" trên Home (không
  có mockup nên tự thiết kế theo token animike-design), mỗi item ghép cặp 2
  anime (mỗi ảnh bấm riêng mở đúng Detail của anime đó) kèm lý do do user MAL
  viết + username. Verify qua curl: `mal_id` là chuỗi ghép dạng `"30-51552"`
  (không phải Int như list khác), `entry` LUÔN có đúng 2 phần tử, và — KHÁC
  `/watch/episodes*` — endpoint này phân trang THẬT (100 item/trang, `page=1`/
  `page=2` trả data khác nhau, 20 trang). Preview Home dùng SWR + Room cache
  riêng (`cached_community_recommendation`, TTL 24h giống Reviews vì user
  đăng liên tục — không dài như Recommendations/Pictures/Characters vốn gần
  như tĩnh); "Xem tất cả" (`CommunityRecommendationsScreen`, package
  `ui/communityrecommendations`) dùng Paging 3 riêng
  (`CommunityRecommendationsPagingSource`, page size 100), không liên quan
  cache Room preview. Card dùng chung (`CommunityRecommendationCard`) cho cả
  Home preview lẫn "Xem tất cả", tham số `contentMaxLines` khác nhau vì
  width cố định (LazyRow) so với fillMaxWidth (LazyColumn) có chỗ dọc khác
  nhau. Đã cân nhắc `/anime/{id}/recommendations` (đã dùng ở Detail) nhưng đó
  là recommendation THEO 1 anime cụ thể (1 entry, có context sẵn) — khác hẳn
  feed cộng đồng toàn cục này (2 entry ghép cặp, không theo malId nào).
- [x] **Biểu đồ phân bố điểm + số người xem, nhạc OP/ED** ✅ — 2 tính năng mới
  (không có mockup riêng, tự thiết kế theo token animike-design), NHƯNG sau
  khi làm xong user review lại thấy Detail đã quá nhiều section nên đã đổi vị
  trí đặt so với bản đầu:
  - **Nhạc phim** (`/anime/{id}/themes`): vẫn ở Detail, nhưng gộp vào
    `ExploreTabsSection` (cùng Đề xuất/Đánh giá) làm tab thứ 3 thay vì đứng
    riêng — `DetailTab` giờ có 3 giá trị, TabRow chỉ hiện những tab THỰC SỰ
    có data (không còn so cứng "2 tab" như bản gốc UI-4). Bỏ hẳn thu gọn/xem
    thêm (không phù hợp khi đã nằm trong tab, nội dung tab vốn đã tách khỏi
    luồng cuộn chính) — thay bằng 1 nút nổi "cuộn lên đầu trang" ở cấp
    `DetailScreenContent` (hiện khi `listState.firstVisibleItemIndex` vượt
    ngưỡng), giải quyết đúng lo ngại ban đầu (anime dài tập như One Piece có
    thể 15-20+ OP/ED làm trang dài hẳn ra).
  - **Thống kê** (`/anime/{id}/statistics`): CHUYỂN hẳn sang màn Đánh giá "Xem
    tất cả" (`ReviewsScreen`) — hiện làm header phía trên danh sách Đánh giá
    phân trang, tương tự pattern quen thuộc của Play Store/App Store (hiện
    histogram điểm ngay trên đầu trang review đầy đủ). Lý do: điểm tổng
    (`detail.score`) đã hiển thị sẵn ở badge Hero của Detail rồi, còn phần
    phân bố chi tiết + số người xem hợp lý hơn khi đặt cạnh Đánh giá (cùng
    nhóm "ý kiến cộng đồng"), đồng thời giảm bớt 1 section cho Detail.
    `ReviewsViewModel` giờ có `ReviewsState` thật (trước là `data object`
    rỗng) chứa `statistics: AnimeStatistics?`, tự observe/refresh Room khi
    vào màn — độc lập với `items` (Paging 3), không liên quan loadState của
    danh sách review.
  - Cache Room: cả 2 vẫn 1 row/anime theo pattern `CachedAnimeDetailEntity`
    (`@PrimaryKey malId`), 2 bảng `cached_anime_statistics`/`cached_anime_themes`,
    TTL 7 ngày (`CacheTtl.STATISTICS_MS`/`THEMES_MS`), danh sách con (10 entry
    điểm, openings/endings) encode delimiter ASCII giống
    `CachedAnimeDetailMapper` — KHÔNG đổi so với bản đầu, chỉ đổi ViewModel
    nào gọi observe/refresh (Statistics: `DetailViewModel` → `ReviewsViewModel`;
    Themes: vẫn `DetailViewModel`). DB version 8→9.
- [x] **Nâng cao màn Đánh giá** ✅ — `/anime/{id}/reviews` thật ra trả nhiều
  field hơn `ReviewDto` ban đầu khai (chỉ mal_id/review/score/user.username);
  verify qua curl phát hiện: `reactions` (8 loại: overall/nice/love_it/funny/
  confusing/informative/well_written/creative), `date`, `tags` (LUÔN đúng 1
  phần tử — **3 giá trị**, KHÁC giả định ban đầu của user chỉ có 2:
  Recommended/**Mixed Feelings**/Not Recommended), `user.images` (avatar).
  - `AnimeReview` mở rộng thêm `userAvatarUrl`/`date`/`tag: ReviewTag?`/
    `reactions: ReviewReactions?` — đều nullable vì preview cache ở Detail
    (`cached_review_preview`, top 5) KHÔNG lưu các field này (không mở rộng
    schema chỉ để chứa dữ liệu Detail's ReviewCard không hiển thị) — chỉ
    `ReviewsScreen` (Paging, live) mới có đủ. `date` format sẵn ở mapper
    (`dd/MM/yyyy`, `Locale("vi","VN")`) qua `SimpleDateFormat` (KHÔNG dùng
    `java.time` vì minSdk 24 chưa bật core library desugaring).
  - **ReviewCard** (list) nâng cấp: avatar tròn + username + ngày đăng, badge
    tag tự thiết kế (xanh success/tertiary/error theo 3 mức), dòng reactions
    rút gọn (chỉ `overall`), vẫn giữ maxLines=6 cho review text. Bấm vào card
    mở `ReviewDetailScreen` (route `reviews/detail`, KHÔNG bottom sheet — app
    chưa có tiền lệ bottom sheet nào, mọi "xem thêm" đều dùng dedicated
    screen, và review text có thể rất dài nên full-screen đọc tốt hơn) xem
    đầy đủ: review KHÔNG bị cắt dòng, breakdown đủ cả 7 loại reactions.
  - `ReviewDetailScreen` dùng chung `ReviewsViewModel` (scope theo backstack
    entry của `Routes.REVIEWS`, giống `SearchFilterScreen`/`SearchViewModel`)
    thay vì tự fetch lại — Paging 3 không có cách tra cứu "item theo id" nên
    review được lưu thẳng vào `ReviewsState.selectedReview` lúc bấm card
    (event `OnReviewClick`), không truyền qua nav argument.
  - "Thống kê" đổi từ sibling cố định phía trên thành item ĐẦU trong chính
    `LazyColumn` của danh sách review (theo yêu cầu user) — cuộn xuống thì
    thống kê cuộn theo luôn thay vì chiếm chỗ cố định trên đầu màn hình.
- [x] **Nút "Xem trên..." + tab "Video"** ✅ — mục cuối cùng của MVP4 (chuyển
  từ MVP 3 cũ), verify cả 2 endpoint qua curl (anime 1 + 20) trước khi code:
  - **"Xem trên..."** (`/anime/{id}/streaming`): response chỉ `{name, url}`
    (~1-5 nền tảng hợp pháp: Crunchyroll/Netflix/Tubi...). UI = 1 hàng chip
    cuộn ngang đặt ngay dưới GenreChips (hành động chính của người muốn XEM
    phim, càng gần đầu càng dễ thấy; chip nền primary-nhạt để phân biệt với
    GenreChips thuần hiển thị). Bấm mở browser ngoài qua effect MỚI
    `DetailEffect.OpenExternalUrl` (tách khỏi `OpenYoutube` vì bên đó build
    URL từ videoId đã sanitize, còn đây là URL nguyên bản từ Jikan).
  - **Tab "Video"** (`/anime/{id}/videos`): response là 1 OBJECT
    `{promo, music_videos, episodes}` — mảng `episodes` CỐ Ý bỏ qua không
    khai trong DTO (trùng hoàn toàn `/videos/episodes` đã dùng cho
    EpisodesSection). promo dùng field `trailer`, music_videos dùng field
    `video` — cả 2 cùng shape `TrailerDto` nên tái dùng, và cùng data quirk
    `youtube_id` NULL chỉ có `embed_url` → tái dùng `resolveYoutubeId()` (đổi
    private → internal); `images` cũng LUÔN null nên thumbnail derive từ
    youtubeId (cùng kỹ thuật `trailerThumbnailUrl`). Gộp promo + MV thành 1
    model `AnimeVideo` (subtitle = "bài hát — nghệ sĩ" từ `meta` cho MV, null
    cho promo), hiển thị làm **tab thứ 4** trong `ExploreTabsSection` (cùng
    lý do gộp với Nhạc phim: đỡ dài Detail) — card 16:9 kiểu EpisodeItem kèm
    play overlay như TrailerCard, bấm gửi cùng event `OnTrailerClick` (cùng
    hành vi "mở 1 video YouTube").
  - Cache Room: 2 bảng list mới `cached_streaming_link` (PK malId+url) +
    `cached_anime_video` (PK malId+youtubeId), sentinel row khi rỗng thật,
    TTL 7 ngày (`CacheTtl.STREAMING_MS`/`VIDEOS_MS` — nền tảng phát hành và
    danh sách PV/MV gần như tĩnh sau khi anime lên sóng). DB version 9→10.
    Cả 2 tải tuần tự trong `loadAll` + được pull-to-refresh force cùng nhóm.
  - **Phát hiện sau khi dùng thử**: `/videos.promo[0]` và `/full.trailer`
    (đã hiện riêng ở `TrailerCard`) LÀ CÙNG 1 VIDEO — verify qua curl (anime
    1: cả 2 cùng `embed_url` youtube `gY5nDXOtv_o`) — Jikan trả trùng lặp có
    chủ đích giữa 2 endpoint. Không lọc thì user thấy y hệt 1 trailer ở cả
    "Xem trailer" lẫn đầu tab "Video". Sửa bằng computed property
    `DetailState.tabVideos` (cùng pattern `CharactersState.filteredCharacters`
    — lọc ở State, không phải trong composable) loại video có `youtubeId`
    trùng `detail.trailerYoutubeId`, vẫn giữ các promo/MV KHÁC (PV 2, PV 3,
    music video...).
- [x] **Nâng cao Đánh giá + Hình ảnh ở Detail** ✅ — 2 cải tiến hậu-MVP4:
  - **Tab "Đánh giá" đồng bộ với ReviewsScreen**: trước đây tab preview ở
    Detail dùng bản `ReviewCard` đơn giản (username/score/text, không click)
    trong khi ReviewsScreen đã có avatar/date/tag/reactions + click mở
    `ReviewDetailScreen`. Theo yêu cầu user: đồng bộ 2 nơi. Thực hiện:
    - Extract `ReviewCard`/`ReviewTagBadge` thành component dùng chung ở
      `ui/components/ReviewCard.kt` (dùng ở cả ReviewsScreen lẫn tab Detail).
    - Mở rộng `CachedReviewPreviewEntity` (preview top-5 ở Detail) thêm
      `userAvatarUrl`/`date`/`tag`/`reactionsEncoded` — trước đây CỐ TÌNH chỉ
      lưu 4 field tối thiểu (quyết định đó nay đảo ngược vì user muốn đồng bộ
      hiển thị thật, không chỉ đồng bộ component). `reactionsEncoded` dùng
      delimiter ASCII (8 số nối bằng `:::`) — cùng kỹ thuật
      `CachedAnimeStatisticsMapper`. DB version 10→11.
    - `ReviewDetailScreen` đổi từ "nhận `ViewModel: ReviewsViewModel`" (khi
      mới tạo, chỉ 1 nơi mở) thành **stateless** — nhận thẳng `review:
      AnimeReview?` làm tham số, vì giờ có 2 nơi mở màn này với 2 nguồn khác
      nhau (ReviewsViewModel.selectedReview / DetailViewModel.selectedReview).
      2 route riêng cùng trỏ 1 composable: `Routes.REVIEW_DETAIL` (từ
      ReviewsScreen, đọc ReviewsViewModel qua backstack entry của
      `Routes.REVIEWS`) và `Routes.DETAIL_REVIEW_DETAIL` (từ tab Detail, đọc
      DetailViewModel qua backstack entry của `Routes.DETAIL`) — route tách
      riêng vì `getBackStackEntry(Routes.REVIEWS)` sẽ crash nếu gọi từ luồng
      Detail (không đứng sau Routes.REVIEWS trên backstack).
    - `DetailContract` thêm `selectedReview`/`OnReviewClick`/
      `NavigateToReviewDetail` — cùng pattern với `ReviewsContract`.
  - **Viewer "Hình ảnh" hiện ảnh nét nhất + nút tải xuống**: trước đây
    `PicturesSection`/`PictureViewerDialog` dùng CHUNG 1 url (đã ưu tiên
    `large_image_url`) cho cả grid preview lẫn viewer full-screen — nghĩa là
    grid phải tải ảnh độ phân giải lớn chỉ để hiển thị thumbnail 2:3 nhỏ.
    Theo yêu cầu user: viewer phải hiện ảnh nét nhất (tường minh, không tình
    cờ) + có nút tải xuống. Thực hiện:
    - Domain model mới `Picture(thumbnailUrl, fullUrl)` thay `String` đơn —
      `thumbnailUrl` = `image_url` (vừa, cho grid), `fullUrl` =
      `large_image_url` ưu tiên (cho viewer). `CachedPictureEntity` đổi PK từ
      `url` sang `fullUrl`. DB version 11→12.
    - Viewer thêm nút "⬇" (top-start, đối xứng nút đóng "✕" top-end — bố cục
      toolbar quen thuộc kiểu Google Photos) gửi `DetailEvent.
      OnDownloadPictureClick(fullUrl)` → `DetailEffect.DownloadPicture` →
      `DownloadManager.enqueue(...)` lưu vào `DIRECTORY_PICTURES` công khai
      (Photos/Gallery tự quét thấy) kèm Toast xác nhận. Thêm quyền
      `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"` trong Manifest — API 29+
      scoped storage không cần quyền này cho DownloadManager ghi thư mục
      công khai).
      **Sửa qua review**: bản đầu chỉ khai quyền trong Manifest — SAI, vì
      `WRITE_EXTERNAL_STORAGE` là dangerous permission từ API 23 nên vẫn phải
      xin RUNTIME trên API 23-28 thật (miễn trừ scoped-storage áp dụng theo
      API của THIẾT BỊ đang chạy, không phải targetSdk của app), nếu không
      nút tải sẽ LUÔN thất bại (không phải hiếm) trên mọi máy API 24-28. Sửa
      bằng `rememberLauncherForActivityResult(RequestPermission())` trong
      `DetailScreen`, chỉ xin quyền khi `SDK_INT` nằm trong khoảng 23..28 và
      chưa được cấp; API 29+ gọi thẳng không cần xin.
**Thứ tự triển khai đã thống nhất với user (2026-07)** — không có mockup ở
`docs/UI/` cho cả 4 màn dưới đây (đã confirm qua Glob), tự thiết kế theo token
`animike-design`. Shape endpoint đã verify sẵn ở
`.claude/skills/jikan-api/references/mvp5-characters-people-studio.md` — xem
trước khi code, không curl lại. Thứ tự dựa trên "entry point nào có sẵn/rẻ
nhất" chứ không theo thứ tự liệt kê gốc trong roadmap:

- [x] **1. Character Detail** (`/characters/{id}/full`) ✅ — làm trước tiên vì
  entry point rẻ nhất: `CharactersScreen` card CHƯA có `onClick` trước đó, chỉ
  cần thêm tap handler vào card có sẵn (cả CharactersScreen lẫn tab preview ở
  Detail) thay vì dựng UI mới. Thiết kế: hero portrait full-bleed (dùng chung
  `Dimens.HeroHeaderHeight` với AnimeDetail cho nhất quán) + gradient đáy, tên +
  `name_kanji` đè đáy ảnh, badge `favorites` hình trái tim màu `secondary` góc
  trên-phải (ẩn khi `favorites=0`, cùng quy tắc ẩn "N/A" của ScoreBadge), chip
  ngang `nicknames`. 2 hàng ngang "Xuất hiện trong" (tái dùng `AnimeCard`, ẩn
  badge điểm/năm vì endpoint không trả) và "Lồng tiếng bởi" (voices — CHƯA có
  onClick, chờ People Detail mục 2). `about` thu gọn/xem thêm — trích
  `ExpandableText` (trước đây `private` trong DetailScreen.kt) thành component
  dùng chung ở `ui/components/` vì giờ có 2 nơi dùng.
  - Kiến trúc: tách hẳn `CharacterDetailRepository` riêng (KHÔNG nhét vào
    `AnimeDetailRepository` vốn đã có 10 cặp observe/refresh cho aggregate
    "anime") — aggregate mới khoá theo `characterId`. Domain `CharacterDetail`
    CHỈ chứa field scalar (name/nameKanji/imageUrl/nicknames/favorites/about),
    giống `AnimeDetail` không ôm theo characters/recommendations — 2 danh sách
    "Xuất hiện trong"/"Lồng tiếng bởi" là 2 Flow riêng dù cùng đến từ 1 API
    call, vì shape có cấu trúc (không phải string) nên không delimiter-encode
    chung 1 row được (khác genres).
  - Cache Room: 3 bảng — `cached_character_detail` (core, 1 row/nhân vật,
    nicknames encode delimiter ASCII giống genresEncoded), `cached_character_
    anime_appearance` + `cached_character_voice_actor` (list, PK composite
    `characterId`+id con, distinctBy chống trùng). CHỈ core có `getFetchedAt`
    — 2 bảng list không cần vì cả 3 luôn ghi cùng lúc trong 1 lần gọi
    `/characters/{id}/full` (khác Statistics/Themes là 2 API riêng), và cũng
    KHÔNG cần sentinel row khi rỗng thật vì TTL gate không phụ thuộc chúng.
    TTL 7 ngày (`CacheTtl.CHARACTER_DETAIL_MS`, giống Characters/Recommendations).
    **Sửa qua review**: bản đầu ghi 3 bảng tuần tự KHÔNG bọc transaction — nếu
    coroutine bị cancel giữa chừng hoặc 1 trong 2 `replace()` sau ném exception,
    bảng core đã commit (TTL "tươi") nhưng 2 bảng danh sách chưa ghi kịp → lần
    refresh sau thấy core còn tươi nên bỏ qua gọi lại API, 2 section kẹt sai
    suốt 7 ngày mà user không có cách tự sửa. Sửa bằng `AppDatabase.
    withTransaction { }` bọc cả 3 lệnh ghi (`room-ktx`), khớp đúng giả định "TTL
    gate dùng chung 1 bảng chỉ đúng nếu 3 bảng luôn sống/chết cùng nhau".
    Cũng sửa `nicknames` chưa `distinct()` trước khi dùng làm `LazyRow` key
    (crash "Key ... was already used" nếu Jikan trả nickname trùng).
  - Route `character/{characterId}` (arg tên `characterId`, KHÁC `malId` — nhấn
    mạnh đây là aggregate khác, tránh nhầm lẫn khi đọc code điều hướng).
  - **Phát sinh sau khi dùng thử (user report)**: bấm qua lại giữa 1 anime và 1
    nhân vật xuất hiện trong đó (Anime Detail → "Xuất hiện trong" → Character
    Detail → "Xuất hiện trong" → cùng Anime Detail đó → ...) cứ push thêm 1
    bản Detail/Character Detail MỚI vào back stack mỗi lần bấm — back stack
    phình vô hạn, cảm giác như "vòng lặp màn hình". Cùng lỗi tiềm ẩn với chuỗi
    Đề xuất (Anime Detail → Đề xuất → Anime Detail khác → ...) vốn đã tồn tại
    từ trước, chỉ là Character Detail làm lộ rõ ra. Sửa bằng
    `NavController.navigateOrPopToExisting()` (helper mới ở
    `AniMikeNavHost.kt`) — dùng `popBackStack(route, inclusive = false)` (API
    public của Navigation, tự so khớp route ĐÃ ĐIỀN THAM SỐ THỰC, giống hệt
    cách `navigate(route)` chọn destination, nên không đụng nhầm anime/nhân
    vật KHÁC id) trước; nếu tìm thấy (trả `true`) thì đã tự pop tới đúng entry
    đó (tương đương user tự bấm back nhiều lần) thay vì push bản trùng, nếu
    không (`false`) mới `navigate()` bình thường. Áp dụng cho MỌI lệnh
    `navController.navigate(Routes.detail(...))`/`Routes.characterDetail(...)`
    trong `AniMikeNavHost.kt` (kể cả chuỗi Đề xuất Anime↔Anime), không chỉ
    riêng case Character Detail.
    **Sửa qua review (vòng 2)**: bản đầu tự viết vòng lặp `popBackStack()` +
    tự so khớp qua `NavController.currentBackStack` — property đó bị đánh dấu
    `@RestrictedApi` (phải thêm `@SuppressLint` mới build được), dấu hiệu nên
    tránh dùng trong app code. Đổi sang `popBackStack(route, inclusive)` built-in
    ở trên — bỏ được `@SuppressLint`, vòng `while` tự viết, và tham số `isMatch`
    lambda (framework tự lo phần so khớp, đã được Navigation team test).
- [x] **2. People/Seiyuu Detail** (`/people/{id}/full`, `/anime/{id}/staff`) ✅
  — 2 entry point: "Lồng tiếng bởi" ở Character Detail (trước đó CỐ TÌNH chưa
  có onClick, giờ nối vào) và mục MỚI "Ê-kíp sản xuất" ở Detail
  (`/anime/{id}/staff`). Thiết kế: cấu trúc hero portrait giống Character
  Detail nhưng đổi accent sang `primary` (tím) để phân biệt "người thật" khỏi
  "nhân vật hư cấu" — cùng badge `favorites` (♥, giờ màu `primary`), chip
  `alternate_names` (giống nicknames). Sáng tạo riêng cho People Detail
  (không có ở Character Detail):
  - **Stat strip** ngay dưới hero: 2 chip nhanh "🎙 N vai diễn"/"🎬 N tác phẩm"
    (nền `primary` alpha 15%) — cho cảm giác "career snapshot" tức thì, ẩn chip
    nào bằng 0.
  - **2 tab** "Vai trò sản xuất"/"Vai diễn lồng tiếng" (TabRow Material3, cùng
    style `DetailTabRow`) thay vì 2 section xếp chồng — gọn hơn khi 1 người có
    cả 2 loại credit.
  - **"Vai diễn lồng tiếng" bắt buộc local search** — `voices[]` có thể tới
    **541 item** (đã verify, KHÔNG pagination), lọc theo tên nhân vật/tên anime
    ngay trong bộ nhớ (computed `filteredVoiceRoles`, cùng pattern
    `CharactersState.filteredCharacters`). **Cố tình KHÔNG dùng `AnimatedContent`
    crossfade** khi đổi tab (khác `ExploreTabsSection` ở DetailScreen) — list
    541 item phải nằm trong `items()` của chính `LazyColumn` ngoài cùng để lazy
    load/recycle thật sự; bọc trong `AnimatedContent` sẽ ép compose hết 1 lần,
    mất lợi ích lazy. Đánh đổi: mất hiệu ứng chuyển tab, giữ hiệu năng.
  - **(Đã thay bằng nhóm theo anime — xem "Cải tiến sau khi dùng thử" bên
    dưới)** ~~Duo thumbnail cho mỗi dòng vai diễn: poster anime (2:3 thu nhỏ) +
    avatar nhân vật đè góc dưới-phải~~.
  - Kiến trúc: `PersonDetailRepository` riêng (khoá `personMalId`, KHÁC
    `malId`/`characterId`) — áp dụng NGAY bài học atomicity từ Character
    Detail: 3 bảng Room (core + 2 list, KHÔNG sentinel row) ghi trong 1
    `database.withTransaction { }` từ đầu, không phải sửa lại qua review lần
    nữa. "Ê-kíp sản xuất" (`AnimeStaffMember`) lại thuộc `AnimeDetailRepository`
    (khoá theo `malId` của anime, giống Characters/Recommendations — CÓ
    sentinel row vì gate TTL độc lập trên chính bảng `cached_anime_staff_member`).
  - TTL 7 ngày cho cả 2 (`CacheTtl.PERSON_DETAIL_MS`/`STAFF_MS`). Route
    `person/{personId}` (arg riêng, aggregate thứ 3 sau anime/nhân vật). DB
    version 13→14 (4 bảng mới: `cached_person_detail`,
    `cached_person_staff_credit`, `cached_person_voice_role`,
    `cached_anime_staff_member`).
  - **Sửa qua review**: 2 vấn đề thật (không phải nitpick):
    - **Mất dữ liệu credit**: bản đầu `PersonStaffCredit.position: String` +
      `distinctBy { it.anime.malId }` — nếu 1 người giữ NHIỀU vai trò trên
      CÙNG 1 anime (Jikan trả 2 entry riêng cùng `anime.mal_id`, khác
      `position`, VD đạo diễn kiêm storyboard), `distinctBy` sẽ ÂM THẦM XOÁ 1
      credit thật. Sửa bằng `positions: List<String>` (đổi thành list, cùng
      pattern `AnimeStaffMember`) + `List<PersonStaffRefDto>.toStaffCredits()`
      (hàm mức-LIST mới ở `PersonDetailMapper.kt`, gộp theo `anime.malId`
      thay vì lọc theo item đơn).
    - **Vị trí cuộn không reset khi đổi tab**: 2 tab "Vai trò sản xuất"/"Vai
      diễn lồng tiếng" nằm CHUNG 1 `LazyColumn` (cố tình, xem lý do
      AnimatedContent ở trên) nên đổi tab không tự cuộn về đầu — cuộn sâu ở
      tab 541 item rồi đổi sang tab ~15 item sẽ bị clamp/lạc vị trí. Sửa bằng
      `LaunchedEffect(effectiveTab) { listState.animateScrollToItem(0) }`.
  - **Cải tiến sau khi dùng thử (user góp ý, 2026-07)**:
    - **Ngày sinh format dd/MM/yyyy** — `PersonDetailMapper.formatBirthday()`
      (cùng kỹ thuật `ReviewMapper.formatReviewDate`, `SimpleDateFormat`
      không thread-safe nên tạo instance mới mỗi lần gọi).
    - **"Vai trò sản xuất" đổi từ `LazyRow` ngang sang lưới dọc 3 cột** — chunked
      thành từng hàng, mỗi hàng vẫn là 1 `item{}` riêng của chính `LazyColumn`
      ngoài cùng (KHÔNG lồng `LazyVerticalGrid` vào `LazyColumn` — vi phạm
      nested scrollable), giữ lazy theo hàng.
    - **Nút nổi "cuộn lên đầu" cho CẢ 2 tab** — trích `ScrollToTopButton` từ
      `DetailScreen.kt` (trước đó `private`) thành component dùng chung ở
      `ui/components/` (2 nơi dùng). Áp dụng tự nhiên cho cả 2 tab vì dùng
      chung 1 `listState`/`LazyColumn`, không cần code riêng cho từng tab.
    - **"Vai diễn lồng tiếng" nhóm theo anime + sort Chính→Phụ** — thay hẳn
      "duo thumbnail" (poster+avatar đè) bằng bố cục nhóm. Model mới
      `VoiceRoleGroup` + computed `PersonDetailState.groupedVoiceRoles` (group
      theo `anime.malId` bằng `LinkedHashMap` giữ thứ tự xuất hiện đầu,
      `sortedBy` ổn định trong nhóm). Bản đầu là header phẳng + dòng thụt lề —
      user chê kém sinh động, làm lại thành **CARD nổi trên nền `surface`**:
      poster 2:3 lớn (64x96, token `VoiceRoleGroupPosterWidth/Height`) bên
      trái làm điểm nhấn, tên anime + dòng nhân vật bên phải, avatar vai
      CHÍNH có **ring màu `primary`** (token `BorderThin` mới, thay hardcode
      2.dp) phân biệt tức thì với vai phụ; cả card là 1 vùng bấm
      (onClickLabel + Role.Button — fix a11y từ review vì mất cue duo cũ).
    - **Fix lỗi biên dịch user phát hiện**: `remember(staffCredits)` cho
      chunked rows bị đặt TRONG content lambda của `LazyColumn`
      (`LazyListScope` KHÔNG phải @Composable context — lỗi "@Composable
      invocations can only happen from...") — chuyển lên thân composable
      `PersonDetailContent`.
    - **Ô tìm kiếm ẩn khi danh sách ngắn** — chỉ hiện khi `voiceRoles.size >= 15`
      (dùng size GỐC, không phải đã lọc, để ô không biến mất giữa chừng lúc
      gõ). Khi focus, `BringIntoViewRequester` tự cuộn `LazyColumn` cha đẩy ô
      lên trên (kết hợp `Modifier.imePadding()` ở Box ngoài cùng để tính đúng
      khoảng bàn phím che) — tránh bàn phím che mất danh sách bên dưới.
- [x] **3. Studio Detail** (`/producers/{id}/full`) ✅ — bấm chip studio ở
  Detail để mở. Phần "nặng" nhất: phải đổi `AnimeDetail.studios` từ `String`
  join sẵn → `List<Studio>` (Studio = malId + name) để bấm được:
  - Chuỗi thay đổi: `NamedResourceDto` thêm `mal_id` (nullable, genres cũng
    dùng DTO này nhưng chỉ đọc name nên không ảnh hưởng) → `AnimeDetailMapper`
    lọc studio đủ malId+name → `CachedAnimeDetailEntity.studios: String` đổi
    thành `studiosEncoded` (mỗi studio "malId:::name" nối `~~~`, cùng kỹ thuật
    relations) → DB version 14→15. Ở Detail, studio TÁCH khỏi `detailMetaLine`
    (chuỗi text) thành `StudiosRow` chip bấm được (nền primary-nhạt như
    StreamingLinksRow) đặt ngay dưới GenreChips.
  - Thiết kế màn Studio Detail: **logo làm điểm nhấn** (KHÁC hero ảnh
    full-bleed của Character/Person Detail) — logo studio thường nền trong
    suốt/vuông nên đặt trên card `surface` bo góc + `ContentScale.Fit` (không
    Crop, tránh méo logo), căn giữa. Dưới logo: tên studio + **stat row** 3
    chip (🎬 count anime / 📅 Est. năm / ♥ favorites, ẩn chip thiếu data) +
    hàng chip link `external[]` (🔗, bấm mở browser ngoài qua
    `OpenExternalUrl`, URL đã lọc http/https ở mapper) + `about` thu gọn/xem
    thêm (`ExpandableText`).
  - Danh sách anime studio sản xuất: **TÁI DÙNG `searchAnime` + `producers={id}`**
    (chỉ thêm 1 query param, KHÔNG endpoint/DTO mới) qua
    `StudioAnimePagingSource` (Paging 3, khuôn `AnimeSearchPagingSource`).
    Header (logo/stat/external/about) là các item **full-span** ĐẦU của chính
    `LazyVerticalGrid` 3 cột chứa anime — cuộn cùng lưới (pattern ReviewsScreen:
    header là item đầu của list paged), tự xử lý loading/empty/error/append.
  - Kiến trúc: `StudioDetailRepository` CHỈ 1 bảng core `cached_studio_detail`
    (SWR, TTL 7 ngày `STUDIO_DETAIL_MS`) — KHÔNG có bảng list như Character/
    Person Detail vì danh sách anime đi Paging (không cache), nên cũng không
    cần transaction đa-bảng. Route `studio/{studioId}` (arg riêng, aggregate
    thứ 4). Điều hướng dùng `navigateToStudioDetail` (cùng `navigateOrPopToExisting`
    dedup — studio→anime→studio cũng có thể lặp như các cặp khác).
  - Review clean (không blocker). Sửa 1 a11y: `StatChip` thêm
    `clearAndSetSemantics { contentDescription }` để TalkBack đọc câu đủ nghĩa
    ("6393 lượt thích") thay vì ghép glyph emoji thô ("trái tim, 6393").
    **Nhận biết + chấp nhận có ý thức**: `StudioAnimePagingSource` chỉ
    `distinctBy { malId }` TRONG 1 trang (không cross-page) — nếu Jikan search
    trả trùng mal_id giữa 2 trang, `itemKey { malId }` có thể "key already
    used". Đây là rủi ro CÓ SẴN, dùng chung với `AnimeSearchPagingSource`
    (Search/Season/Schedule) — không sửa riêng ở đây để không lệch pattern;
    khi làm nên fix cross-page cho CẢ cụm search paging 1 lần.
- [x] **4. "Top nhân vật"** (`/top/characters`, discovery) ✅ — **Entry point:
  section "Nhân vật nổi bật" trên Home** (user chọn phương án này thay vì tab
  riêng/Search — nhất quán với "Đề xuất cộng đồng"/"Tập mới", không phá giới
  hạn 3-4 tab của animike-design). Home preview: hàng ngang SWR + Room cache
  (`cached_top_character`, 1 feed toàn cục giống New Episodes, page 1 cắt còn
  15, TTL 24h `TOP_CHARACTERS_MS`); "Xem tất cả" mở `TopCharactersScreen`
  (Paging 3 — endpoint phân trang thật, 3254 trang). Bấm 1 nhân vật (cả
  preview lẫn lưới) mở thẳng Character Detail (mục 1, đã có).
  - Thiết kế: lưới portrait **2 cột** (khác 3 cột AnimePagingGrid vì ảnh nhân
    vật dọc hơn), **rank ribbon top-3** (huy hiệu vàng/bạc/đồng "#1/#2/#3" —
    thêm token `rankGold/rankSilver/rankBronze` vào Color.kt/Theme.kt như
    `success`, KHÔNG hardcode; không phá quy tắc 1-accent vì là màu ngữ nghĩa
    medal), badge `favorites` "♥ 180K" (format rút gọn K/M) overlay góc
    dưới-trái ảnh. `TopCharacterCard` dùng CHUNG Home preview + lưới (rank=null
    ở preview → ẩn ribbon).
  - `TopCharactersPagingSource` khử trùng malId **XUYÊN trang** (`seenIds`,
    pattern `CommunityRecommendationsPagingSource`) — tránh "key already used".
    DB version 15→16. Route `topCharacters` (không arg, feed toàn cục).

**→ MVP 5 (Nhân vật / Người / Studio) HOÀN THÀNH ✅** — 4/4 màn: Character
Detail, People/Seiyuu Detail, Studio Detail, Top nhân vật. Tiếp theo: MVP 6.

### MVP 6 — Tracking local
- [x] **Đợt 1 — Trạng thái xem** ✅ (Đang xem / Đã xem / Tạm dừng / Bỏ / Dự
  định xem, thuần Room không API):
  - Data: bảng `tracking` (PK malId, snapshot title/ảnh/score/year như
    `favorite` để "Danh sách" hiển thị không cần API; cột
    `episodesWatched`/`personalScore` KHAI SẴN cho đợt 2 — tránh bump schema
    2 lần, cả 3 cột tracking đều nullable, row bị xoá khi cả 3 null).
    `TrackingDao.toggleStatus` gộp đọc+ghi @Transaction (chống TOCTOU
    double-tap, cùng kỹ thuật `FavoriteDao.toggle`): chọn status mới = set,
    bấm LẠI status đang chọn = bỏ theo dõi. `TrackingRepository` local-only
    (nuốt lỗi + log như FavoriteRepository). DB version 16→17.
  - **Detail — `WatchStatusButton` trên TopBar** (làm lại theo góp ý user
    sau khi build thử — bản đầu là card `TrackingStatusBar` full-width dưới
    hero, user chê Detail đã quá nhiều section): nút tròn thứ 2 cạnh nút ♥
    (cùng style nền surface bán trong suốt), 🔖 trung tính khi chưa theo dõi,
    đổi thành emoji + màu ngữ nghĩa của trạng thái khi đã set; bấm mở
    **DropdownMenu** (pattern M3 chuẩn cho action trên toolbar, tiền lệ
    DropdownMenu đầu tiên của app) — gom cả 2 hành động cá nhân (trạng thái +
    yêu thích) về góc trên-phải, trả lại không gian nội dung. **Mỗi trạng
    thái 1 màu ngữ nghĩa riêng** (mapping dùng chung
    `ui/components/WatchStatusUi.kt`): Đang xem = success, Đã xem = tertiary,
    Tạm dừng = warning (alias của rankGold), Bỏ = error, Dự định xem = primary.
  - **Lọc trạng thái theo tình trạng phát sóng** (user góp ý: phim chưa chiếu
    không thể "Đang xem/Đã xem") — computed `DetailState.availableWatchStatuses`:
    sắp chiếu ("Not yet aired") → chỉ Dự định xem; đang chiếu → ẩn Đã xem
    (giống MAL); chiếu xong → đủ 5. Trạng thái ĐANG set luôn giữ trong menu
    (kể cả khi không còn hợp lệ do dữ liệu Jikan đổi) để user bỏ được. Đợt 2
    (tiến độ tập) cũng sẽ theo luật này — phim chưa chiếu không có stepper.
  - **Review vòng 2 (redesign) clean, đã sửa 4 mục 🟡/🟢**: tách hằng
    `JIKAN_STATUS_NOT_YET_AIRED` (kèm ghi chú 3 giá trị `status` của Jikan vào
    `jikan-api/SKILL.md` — `"Currently Airing"`/`"Finished Airing"` đã
    curl-verify 2026-07-12, `"Not yet aired"` CHƯA verify được do
    `/seasons/upcoming` bị 504 tạm thời từ Jikan/MAL lúc verify, cần curl lại
    khi có dịp); `WatchStatusButton` thêm `modifier` param cho nhất quán với
    Back/FavoriteButton; sửa comment lỗi thời nhắc `TrackingStatusBar` (đã bị
    xoá) trong `WatchStatusUi.kt`; trạng thái lạc khỏi danh sách hợp lệ giờ
    chèn đúng theo `ordinal` thay vì luôn nhảy lên đầu. Ghi nhận (không sửa,
    cần verify bằng mắt): emoji 🕒/🔖 là pictograph nhiều màu nên
    `Text(color=...)` không tô lại được glyph (đã có tiền lệ 📅 ở
    StudioDetail) — nhãn chữ cạnh nó vẫn lên đúng màu, chỉ glyph giữ màu gốc
    hệ điều hành.
  - **"Danh sách" nâng thành THƯ VIỆN**: hợp nhất (union) favorites +
    tracking qua `combine` 2 Flow trong `FavoritesViewModel` — anime chỉ có
    trạng thái (không yêu thích) giờ cũng xuất hiện. Hàng **filter chip**:
    Tất cả / ♥ Yêu thích / 5 trạng thái (chip trạng thái CHỈ hiện khi
    count > 0, kèm count, màu ngữ nghĩa). Card poster có **badge trạng thái**
    (emoji + nhãn màu, góc dưới-trái — góc trên-trái đã có score badge) + ♥
    nhỏ góc dưới-phải khi vừa yêu thích vừa có trạng thái. Model
    `LibraryEntry`/`LibraryFilter` (sealed — filter status mang theo enum).
  - **Sửa qua review** (không blocker): (1) hợp thức hoá filter trong
    ViewModel khi entries đổi — chip đang chọn biến mất (bỏ trạng thái của
    anime cuối cùng trong mục đang lọc) thì tự về "Tất cả" thay vì kẹt;
    (2) key chip tường minh (`LibraryFilter.key`) thay `toString()` (R8
    obfuscate tên class làm key lệch giữa các bản build); (3) gọi
    `filteredEntries` 1 lần/recomposition; (4) alias `ColorScheme.warning`
    (trỏ RankGold) cho ON_HOLD thay vì dùng thẳng `rankGold` (semantic
    drift); (5) a11y: stateDescription mở/đóng cho TrackingStatusBar + ẩn
    emoji glyph khỏi TalkBack trong chip.
  - **⚠️ QUAN TRỌNG từ v17**: DB chứa DỮ LIỆU USER THẬT (favorite +
    tracking) — mọi bump schema từ đây phải viết Migration thật
    (AutoMigration được), KHÔNG dựa destructive fallback nữa (sẽ xoá sạch
    tracking user tích lũy). Comment cảnh báo đã đặt tại DatabaseModule.
- [x] **Đợt 2 — Tập đã xem + điểm cá nhân** ✅ (cột Room đã khai sẵn ở đợt 1 —
  KHÔNG bump DB version, vẫn v17):
  - **Data**: `TrackingDao.updateEpisodesWatched`/`updatePersonalScore` (2 hàm
    `@Transaction` mới, cùng pattern `toggleStatus`) — merge giá trị cần đổi
    với row hiện có, giữ nguyên 2 cột tracking còn lại, xoá row nếu cả 3 cột
    về null. `Anime.toTrackingSnapshot()` (mapper mới) — snapshot KHÔNG mang
    theo status/episodesWatched/personalScore (khác `toTrackingEntity` dành
    riêng cho `toggleStatus`), dùng làm candidate merge cho 2 hàm trên.
    `episodesWatched<=0` và `personalScore=null` đều nghĩa là "bỏ" (không lưu
    0, lưu null).
  - **Tiến độ tập** — `EpisodeProgressRow` trong `EpisodesSection`: "Tiến độ:
    X/Y tập" + stepper −/+ (ghi giá trị TUYỆT ĐỐI, không phải delta) + thanh
    ngang (tái dùng style `ScoreBarHeight` của "Thống kê" ở ReviewsScreen).
    Tự ẩn theo `AnimatedVisibility` sẵn có của `EpisodesSection` khi
    `episodes` rỗng (phim chưa chiếu) — KHÔNG cần gate riêng như điểm cá nhân.
  - **Điểm cá nhân** — badge "Bạn: N" (màu primary, cạnh badge "★ MAL" màu
    tertiary) trong `HeroHeader`, gate bởi `DetailState.canRatePersonally`
    (cùng luật `JIKAN_STATUS_NOT_YET_AIRED` với `availableWatchStatuses` —
    hero luôn render bất kể `episodes` rỗng hay không nên PHẢI gate riêng,
    khác tiến độ tập). Bấm mở `PersonalScoreDialog` compact (dialog CĂN GIỮA
    mặc định, KHÁC `PictureViewerDialog` full-screen) — `Slider` M3 1-10
    nguyên (`steps=8`), emoji phản hồi đổi live theo mức đang kéo (1-3 😞,
    4-6 😐, 7-8 🙂, 9-10 🤩), nút "Xoá điểm" chỉ hiện khi đã từng chấm.
  - **ViewModel**: `observeWatchStatus()` đổi tên `observeTracking()` (giờ
    set cả 3 field từ 1 Flow). 2 event handler mới KHÔNG dùng job-guard
    "bỏ-qua-tap" như `OnFavoriteClick`/`OnWatchStatusSelected` (toggle) mà
    dùng `previous?.join()` (chain tuần tự, KHÔNG cancel) — set giá trị tuyệt
    đối tính từ state hiện tại nên phải đợi lượt trước ghi xong mới ghi lượt
    sau, tránh mất tap khi bấm nhanh.
  - **Review clean, đã sửa 1 blocker + 3 mục 🟡**: (blocker) `total` của
    `EpisodeProgressRow` truyền THẲNG `detail.episodes` (Int? — có thể null
    khi Jikan CHƯA biết tổng số tập của anime đang chiếu dài kỳ), KHÔNG
    fallback `episodes.size` như bản đầu — `episodes` trong `DetailState` chỉ
    là trang 1 của `/episodes`, dùng làm mẫu số/trần stepper sẽ khoá "+" sai
    trước khi user xem hết; khi `total == null` giờ hiện "Đã xem: N tập"
    (không mẫu số/progress bar) và "+" không bị khoá trần. (🟡) thêm
    `progressJob`/`scoreJob` + `previous?.join()` để tránh 2 write gần như
    đồng thời commit sai thứ tự lúc bấm stepper nhanh; (🟡) `DetailScreen`
    giờ `remember(viewModel) { viewModel::onEvent }` thay vì tạo bound
    reference mới mỗi lần recompose (rõ hơn từ Đợt 2 vì state đổi liên tục
    hơn khi bấm stepper/chấm điểm); (🟡) gộp 4 chỗ dựng `Anime` từ `detail`
    giống hệt nhau (Favorite/WatchStatus/Progress/Score) thành 1 helper
    `AnimeDetail.toAnimeSnapshot()`.

**→ MVP 6 (Tracking local) HOÀN THÀNH ✅** — 2/2 đợt: Trạng thái xem, Tiến độ
tập + điểm cá nhân.

### Công cụ nội bộ — Debug menu (không thuộc FEATURES.md, chỉ DEBUG build)
- [x] **Đợt 1 — FAB Debug + màn 3 tab** ✅ (chỉ hiện/đăng ký khi
  `BuildConfig.DEBUG`, release là dead code không reachable):
  - **Hạ tầng thu thập** (`debug/` package, `object` thuần không Hilt để cả
    interceptor lẫn global logger chạm tới được): `DebugInspector` giữ 2
    ring-buffer RAM (cap 200 entry, mới nhất ở đầu, `MutableStateFlow.update{}`
    atomic nên thread-safe). `DebugNetworkInterceptor` cắm vào NetworkModule
    SAU Retry/RateLimit (ghi mỗi attempt, đo đúng thời gian HTTP; dùng
    `response.peekBody` để KHÔNG tiêu thụ stream gốc). `AppLog` wrapper thay
    **toàn bộ 11 lời gọi `android.util.Log`** rải rác (repository +
    Detail/Studio screen) — vừa Logcat vừa append buffer, append bị bỏ qua ở
    release.
  - **Tab API**: list request (code/method/thời gian/url), bấm expand xem
    req/resp body (monospace). **Tab Log local**: list log tô màu theo level,
    expand xem stack trace. **Tab Cache**: đếm số row từng bảng (query generic
    qua `sqlite_master`, tự bắt kịp khi thêm entity), nút "Xoá cache" (LOẠI
    TRỪ favorite/tracking — dữ liệu user thật v17) + xoá từng bảng có
    AlertDialog confirm (cảnh báo đậm cho bảng user-data). `DebugRepository`
    truy cập `openHelper.writableDatabase` (lưu ý: bỏ qua invalidation tracker
    — chấp nhận cho tool debug, mở lại màn là thấy).
  - **FAB kéo-thả** ở Scaffold cấp cao nhất `AniMikeNavHost` (nổi trên mọi
    màn, offset tích luỹ theo drag), route `debug` + FAB đều gate
    `BuildConfig.DEBUG`. MVI đầy đủ (`DebugContract`/`ViewModel`/`Screen`).
  - **Review clean (không blocker), đã sửa 3 mục 🟡 + vài 🟢**: FAB dùng
    `launchSingleTop` + ẩn khi đang ở màn Debug (khỏi đẩy trùng đích lên
    backstack); offset FAB đổi sang `rememberSaveable` + kẹp trong biên parent
    (kéo ra ngoài màn là mất nút); `loadStats()` bọc try/catch-finally + spinner
    khi load lần đầu (raw SQL có thể ném, không để `isLoadingStats` kẹt true);
    thêm a11y (`Role.Button` + onClickLabel) cho card expand; đổi tên
    `MAX_BODY_CHARS`→`MAX_BODY_BYTES` (peekBody đếm byte).
- [x] **Đợt 2 — Chi tiết + search + xem dữ liệu cache** ✅ (góp ý user sau khi
  dùng thử Đợt 1):
  - **FAB đẩy cao** né bottom nav bar (token `DebugFabBottomPadding`).
  - **Search mỗi tab**: `DebugSearchBar` (OutlinedTextField) — API lọc theo
    URL/method/code, Log theo tag/message/level, Cache theo tên bảng; query
    giữ trong `DebugState` (3 field), lọc ở UI qua `remember(list, query)`.
  - **Màn chi tiết API** (`DebugNetworkDetail*`, tab API đổi click expand-inline
    → `navigate`): overview (code/method/ms/thời điểm/url) + **headers**
    request/response (đã bổ sung capture ở `DebugNetworkInterceptor` +
    `NetworkLogEntry`) + body **pretty JSON** (`DebugFormat.prettyJsonOrRaw` —
    parse-rồi-encode-lại để khử `\"`/`\/` bị lẫn, không JSON thì raw) + nút
    **Copy body** + **Copy cURL** (`buildCurl`, clipboard + Toast). Entry đọc
    từ ring-buffer theo id, bị xoá khi đang xem → "Log đã bị xoá".
  - **Màn chi tiết bảng cache** (`DebugTableDetail*`, tab Cache click bảng →
    navigate): **schema** (`PRAGMA table_info`: cột + kiểu + 🔑 PK) + **20 dòng
    mẫu** (`SELECT * LIMIT 20`, đọc generic theo `Cursor.getType`, BLOB hiện
    "<blob N B>", value dài cắt maxLines). `DebugRepository.tableDetail`.
  - 2 route mới `debug/network/{id}` (LongType) + `debug/table/{name}`, đều
    gate `BuildConfig.DEBUG`.
  - **Review clean (không blocker), đã sửa 3 🟡 + vài 🟢**: `CacheTab` nhận
    field lẻ thay vì cả `DebugState` (đứng tab Cache mà log mạng vẫn đẩy state
    mới → recompose oan); `remember(entry.id)` cho prettyJson/cURL ở màn chi
    tiết (parse ~8KB không chạy lại mỗi recompose); a11y nút `✕` search
    (contentDescription + ẩn glyph); `launchSingleTop` cho 2 route detail;
    `loadJob?.cancel()` chống retry chồng coroutine; key cho itemsIndexed rows.
  - [ ] **Đợt 3 (nếu cần)**: toggle giả lập 429/timeout + force-network,
    chỉnh TTL runtime qua DataStore, override base URL.

### Xa hơn (xem FEATURES.md mục 2-3)
- **Section 1 + 2 của FEATURES.md coi như XONG.** Còn lại là Section 3 (v2+):
- Manga (3.1, endpoint đã research ở `jikan-api/references/manga-endpoints.md`)
  → Notification lịch chiếu (3.3) → Profile MAL công khai (3.2) → Deep link /
  Widget / Export-import / Đa ngôn ngữ (3.4)

## 5. Quy trình làm việc (cho người mới)

1. **Git**: commit nhỏ, thường xuyên. Làm feature trên branch riêng (`feature/home-screen`), xong merge vào `main`. `main` luôn build được.
2. **Mỗi tính năng làm theo vòng**: DTO/API → Repository → Contract (State/Event/Effect) → ViewModel → UI → test tay → chạy agent `compose-reviewer` → commit.
3. **Đừng làm nhiều tính năng cùng lúc** — xong hẳn 1 màn hình rồi mới sang màn tiếp theo.
4. **Test**: giai đoạn đầu ưu tiên test tay + unit test cho mapper/repository. Chưa cần UI test.
5. **Debug API**: OkHttp logging interceptor ra Logcat + **FAB Debug 🐞** (chỉ debug build) mở màn 3 tab xem API/log/cache ngay trong app — xem mục "Công cụ nội bộ — Debug menu".
6. **Phát hành** (khi sẵn sàng): tạo keystore ký app → build AAB → tài khoản Google Play Console ($25 một lần) → internal testing trước, production sau.

## 6. Tài liệu tham khảo

- Jikan v4 docs: https://docs.api.jikan.moe/
- Base URL: `https://api.jikan.moe/v4`
- Compose: https://developer.android.com/develop/ui/compose
- Paging 3: https://developer.android.com/topic/libraries/architecture/paging/v3-overview
