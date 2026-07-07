# MVP5 — Nhân vật / Người / Studio: endpoint shapes đã verify

Verify 2026-07 bằng curl + PowerShell `ConvertFrom-Json` (xem cách làm ở SKILL.md
mục "Test nhanh endpoint"). Ghi lại đây để KHÔNG phải curl lại khi code MVP5.

## `GET /characters/{id}/full`

Test id=1 (Spike Spiegel).

- `mal_id, url, images, name, name_kanji, nicknames: string[]`
- `favorites: Int` (49135)
- `about: String?` — text tiểu sử dài, có thể chứa nhiều đoạn `\n\n`
- `anime: [{ role: String, anime: { mal_id, url, images, title } }]` — vai trò trong từng phim (3 items ở test case)
- `manga: [{ role, manga: {...} }]` (2 items)
- `voices: [{ language: String, person: { mal_id, url, images, name } }]` — 14 items, có
  cả VA không phải tiếng Nhật (nhưng đa số Japanese)

Không có pagination — response full 1 lần.

## `GET /people/{id}/full`

Test id=1 (Tomokazu Seki).

- `mal_id, url, website_url: String?, images, name, given_name: String?, family_name: String?`
- `alternate_names: string[]`, `birthday: String? (ISO date)`, `favorites: Int` (6278)
- `about: String?`
- `anime: [{ position: String, anime: {...} }]` — credit STAFF (đạo diễn, ADR, v.v.), 15 items ở test case
- `manga: [{ position, manga: {...} }]` (0 items ở test case)
- `voices: [{ role: String, anime: {...}, character: {...} }]` — credit LỒNG TIẾNG, **541 items** ở
  test case (VA lâu năm) → **KHÔNG có pagination trên endpoint này**, trả full 1 lần.
  ⇒ UI "xem tất cả vai đã lồng tiếng" của 1 người PHẢI dùng local-search/filter
  (giống pattern `CharactersScreen` hiện tại lọc `/anime/{id}/characters` tại
  client), KHÔNG dùng Paging 3 vì server không phân trang.

## `GET /anime/{id}/staff`

Test anime id=1 (Cowboy Bebop) — 123 items.

- Response phẳng `{ data: [...] }` — **KHÔNG có field `pagination` trong response**
  (đã xác nhận: JSON top-level chỉ có key `data`).
- Mỗi item: `{ person: { mal_id, url, images, name }, positions: string[] }` — 1 người
  có thể giữ nhiều vai trò cùng lúc (VD `["Director", "Storyboard"]`).

## `GET /producers/{id}/full`

Test id=1 (Studio Pierrot).

- `mal_id, url, titles: [{ type: "Default"|"Japanese"|"Synonym", title: String }]`
- `images.jpg.image_url` (logo), `favorites: Int` (6393)
- `established: String? (ISO date)`, `about: String?`
- `count: Int` (329) — tổng số anime, nhưng **KHÔNG kèm list anime trong response này**
- `external: [{ name: String, url: String }]` — link official site/social/wiki

⇒ Anime list của studio phải gọi riêng, xem mục dưới.

## `GET /anime?producers={id}&page=1` — dùng để list anime của 1 studio

Test producers=1 (Studio Pierrot): `last_visible_page: 66, has_next_page: true,
current_page: 1, items: { count: 5, total: 330, per_page: 5 }`.

Đây là **endpoint search anime hiện có** (`searchAnime`) chỉ thêm query param
`producers`, có pagination THẬT (`last_visible_page`/`has_next_page` chuẩn) →
Studio Detail's anime catalog TÁI SỬ DỤNG được Paging 3 infra đã có sẵn cho
search, chỉ cần thêm tham số `producers` vào request, KHÔNG cần endpoint/DTO mới.

## `GET /top/characters?page=1`

Test page=1 — 81329 total characters, 25/page, `last_visible_page: 3254`.

- Pagination CHUẨN giống mọi list endpoint khác (`pagination.items.total`,
  `has_next_page`, v.v.) → dùng Paging 3 bình thường.
- Mỗi item (shape RÚT GỌN so với `/characters/{id}/full`, không có `anime`,
  `manga`, `voices`): `mal_id, url, images, name, name_kanji, nicknames: string[],
  favorites: Int, about: String?`.
- Sắp xếp mặc định theo `favorites` giảm dần (Lelouch Lamperouge #1 với 180028
  favorites trong test case).
