# Jikan v4 — API Manga (nghiên cứu cho phase Manga)

Manga là **phase sau** của AniMike (xem CLAUDE.md) — file này gom TOÀN BỘ endpoint
manga của Jikan v4 để khi bắt tay làm không phải mò lại docs. Base URL, rate limit,
cấu trúc `data`/`pagination`, quy ước DTO nullable... **giống hệt phần anime** (xem
`../SKILL.md`) — chỉ khác về shape dữ liệu (chương/tập, tác giả, không có seiyuu...).

Nguồn: docs.api.jikan.moe (trang docs render bằng JS nên WebFetch không đọc được
text) → verify shape trực tiếp qua live API. Đánh dấu rõ endpoint nào đã curl-verify
(2026-07-10) vs endpoint mới liệt kê theo tài liệu (⚠️ **verify lại trước khi code**).

---

## 1. Chi tiết manga

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/manga/{id}` | Object manga cơ bản |
| GET | `/manga/{id}/full` | ✅ verify — như trên + `relations[]` + `external[]` |

### Shape `manga/{id}/full` (✅ verify 2026-07-10, id=1 Monster)

```
mal_id, url, images{jpg,webp}, approved,
titles[]{type,title}, title, title_english, title_japanese, title_synonyms[],
type,                       // Manga | Novel | Light Novel | One-shot | Doujinshi | Manhwa | Manhua
chapters (Int?), volumes (Int?),   // ⇔ episodes của anime (đều nullable khi đang ra)
status,                     // Publishing | Finished | On Hiatus | Discontinued | ...
publishing (Boolean),       // ⇔ airing của anime
published{from,to,prop,string},   // ⇔ aired của anime
score, scored, scored_by, rank, popularity, members, favorites,
synopsis, background,
authors[]{mal_id,type,name,url},        // MỚI — người (role Story/Art), map sang People Detail
serializations[]{mal_id,type,name,url}, // MỚI — tạp chí đăng, map sang /magazines
genres[], explicit_genres[], themes[], demographics[],   // {mal_id,type,name,url}
relations[]{relation,entry[]},          // giống anime
external[]{name,url}
```

**KHÁC anime rõ nhất:** `chapters`/`volumes` thay `episodes`; `publishing`/`published`
thay `airing`/`aired`; thêm `authors[]` + `serializations[]` + `demographics[]`;
KHÔNG có `trailer`, `season`, `broadcast`, `studios`, `producers`, `licensors`.

## 2. Sub-resource của 1 manga

| Method | Path | Query | Ghi chú |
|---|---|---|---|
| GET | `/manga/{id}/characters` | — | ✅ verify — `[{character{mal_id,url,images,name}, role}]`. **KHÔNG có `voice_actors`** (manga không có seiyuu) — khác hẳn `/anime/{id}/characters` |
| GET | `/manga/{id}/pictures` | — | Mỗi item `{jpg,webp}` = `ImagesDto` (tái dùng như /anime/{id}/pictures) |
| GET | `/manga/{id}/statistics` | — | `reading, completed, on_hold, dropped, plan_to_read, total, scores[]{score,votes,percentage}` (⇔ watching/plan_to_watch của anime) |
| GET | `/manga/{id}/recommendations` | — | ✅ verify — `[{entry{mal_id,url,images,title}, url, votes}]` (giống anime) |
| GET | `/manga/{id}/reviews` | `page, preliminary, spoilers` | ✅ verify — xem shape dưới |
| GET | `/manga/{id}/relations` | — | `[{relation, entry[]{mal_id,type,name,url}}]` |
| GET | `/manga/{id}/external` | — | `[{name,url}]` link ngoài |
| GET | `/manga/{id}/moreinfo` | — | `{moreinfo: String?}` text bổ sung |
| GET | `/manga/{id}/news` | `page` | ⚠️ tin tức MAL — `[{mal_id,url,title,date,author_username,forum_url,images,comments,excerpt}]` |
| GET | `/manga/{id}/forum` | `filter` (all/other) | ⚠️ chủ đề forum MAL |
| GET | `/manga/{id}/userupdates` | `page` | ⚠️ cập nhật list của user — `[{user, score, status, chapters_read, chapters_total, ...}]` |

### Shape `manga/{id}/reviews` (✅ verify 2026-07-10)

```
[{ mal_id, url, type:"manga",
   reactions{overall,nice,love_it,funny,confusing,informative,well_written,creative},
   date, review (text), score, tags[] ("Recommended"|"Mixed Feelings"|"Not Recommended"),
   is_spoiler, is_preliminary,
   chapters_read (Int?),          // ⇔ episodes_watched của anime review
   user{username,url,images} }]
```

⇒ **Tái dùng gần như nguyên `AnimeReview`/`ReviewReactions`/`ReviewTag`** đã có (MVP4) —
chỉ đổi `episodes_watched` → `chapters_read`. Reactions 8 loại + tags 3 giá trị Y HỆT.

## 3. Khám phá / danh sách

| Method | Path | Query chính | Ghi chú |
|---|---|---|---|
| GET | `/manga` | (search, xem dưới) | ✅ verify pagination + list item |
| GET | `/top/manga` | `type, filter, page, limit` | Top manga. `filter`: `publishing`\|`upcoming`\|`bypopularity`\|`favorite` |
| GET | `/genres/manga` | `filter` | `filter`: `genres`\|`explicit_genres`\|`themes`\|`demographics` |
| GET | `/random/manga` | — | 1 manga ngẫu nhiên (⇔ /random/anime của "Hôm nay đọc gì?") |
| GET | `/magazines` | `page, q, order_by, sort, letter` | Danh sách tạp chí (từ `serializations`) — MỚI, không có bên anime |

### Search `/manga` — query params (⚠️ verify enum trước khi code)

```
q, page, limit,
type,        // manga | novel | lightnovel | oneshot | doujin | manhwa | manhua
score, min_score, max_score,
status,      // publishing | complete | hiatus | discontinued | upcoming
sfw (bool), unapproved (bool),
genres, genres_exclude,   // CSV mal_id (từ /genres/manga)
magazines,                // CSV mal_id
order_by,    // mal_id|title|start_date|end_date|chapters|volumes|score|scored_by|rank|popularity|members|favorites
sort,        // desc | asc
letter,      // 1 ký tự đầu
start_date, end_date       // YYYY-MM-DD (Jikan KHÔNG có param `year` riêng — giống anime, xem MVP3 UI-6)
```

Shape list item (✅ verify, Berserk): giống `manga/full` NHƯNG **không có** `relations`
/`external` (đó là phần "full" thêm). Có đủ `chapters`(null khi đang ra)/`volumes`/
`publishing`/`type`/`score` → đủ để làm 1 `MangaCard` kiểu AnimeCard.

`pagination`: `last_visible_page, has_next_page, current_page, items{count,total,per_page}`
— chuẩn như mọi list, dùng Paging 3 bình thường.

## 4. Feed toàn cục

| Method | Path | Query | Ghi chú |
|---|---|---|---|
| GET | `/recommendations/manga` | `page` | Feed đề xuất cộng đồng toàn cục (⇔ /recommendations/anime của "Đề xuất cộng đồng"), mỗi item ghép cặp 2 manga |
| GET | `/reviews/manga` | `page, preliminary, spoilers` | Feed review manga mới nhất toàn cục |

## 5. Cross-resource đã dùng sẵn (manga-facing)

- `/characters/{id}/full` → có `manga[]{role, manga{mal_id,url,images,title}}` — hiện
  code Character Detail (MVP5) CỐ TÌNH bỏ qua `manga` (chưa làm manga). Khi làm manga,
  bật lại field này.
- `/people/{id}/full` → có `manga[]{position, manga{...}}` (credit tác giả) — tương tự,
  People Detail (MVP5) đang bỏ qua.

---

## Tổng kết cho việc lập kế hoạch phase Manga

**Tái dùng được nhiều (đỡ code):**
- `ImagesDto`, `pagination`, `RelationGroup`, pictures (list `ImagesDto`),
  recommendation pair, review (reactions/tags — chỉ đổi `chapters_read`),
  statistics (histogram điểm giống hệt, chỉ đổi nhãn trạng thái).
- Pattern SWR + Room cache, Paging 3 search, các màn Detail/Characters/Reviews...
  áp thẳng được.

**Phải làm mới:**
- `MangaDetail` domain model (chapters/volumes/publishing/authors/serializations/
  demographics thay episodes/studios/broadcast...).
- `authors[]` → điều hướng sang People Detail (đã có sẵn từ MVP5!); `serializations[]`
  → màn Magazine mới (nếu làm).
- Manga characters KHÔNG có seiyuu → CharacterItem của manga bỏ dòng voice actor.
- KHÔNG có: episodes, videos, streaming, themes (OP/ED), schedule — đó là anime-only.

**Endpoint chưa curl-verify (⚠️ verify trước khi code):** `/manga/{id}` (bản non-full),
`/top/manga`, `/genres/manga`, `/random/manga`, `/magazines`, `/manga/{id}/statistics`,
`/manga/{id}/news`, `/manga/{id}/forum`, `/manga/{id}/userupdates`,
`/manga/{id}/relations`, `/manga/{id}/external`, `/manga/{id}/moreinfo`,
`/recommendations/manga`, `/reviews/manga`, và toàn bộ enum của search `/manga`.
