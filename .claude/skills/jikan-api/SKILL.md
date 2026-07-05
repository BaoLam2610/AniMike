---
name: jikan-api
description: >
  Jikan API v4 integration conventions for AniMike. Use whenever writing or modifying
  networking code, API interfaces, DTOs, repositories, or handling API errors in this
  project. Covers base URL, rate limiting, endpoint map, error handling, and caching rules.
version: 1.0.0
---

# Jikan API v4 — Quy ước tích hợp cho AniMike

Base URL: `https://api.jikan.moe/v4` — không cần API key, **chỉ đọc** (không update được MAL list).

## Rate limit — BẮT BUỘC tuân thủ

- **3 request/giây, 60 request/phút**
- OkHttp Interceptor giới hạn: tối thiểu **400ms giữa 2 request**
- Retry với exponential backoff khi HTTP `429` hoặc `503` (tối đa 3 lần: 1s → 2s → 4s)
- KHÔNG gọi song song nhiều endpoint; trên Home tải tuần tự từng section
- Dữ liệu server cache ~24h → cache local cùng TTL, không refresh liên tục

## Endpoint map (dùng trong app)

| Chức năng | Endpoint |
|---|---|
| Season hiện tại | `GET /seasons/now` |
| Season theo năm/mùa | `GET /seasons/{year}/{season}` (winter/spring/summer/fall) |
| Sắp chiếu | `GET /seasons/upcoming` |
| Top anime | `GET /top/anime?filter=&type=` |
| Lịch chiếu | `GET /schedules?filter={monday..sunday}` |
| Tìm kiếm | `GET /anime?q=&page=&type=&status=&rating=&min_score=&genres=&order_by=&sort=` |
| Chi tiết | `GET /anime/{id}/full` |
| Nhân vật | `GET /anime/{id}/characters` |
| Tập | `GET /anime/{id}/videos/episodes?page=` (có thumbnail, sắp mới→cũ; KHÔNG dùng `/anime/{id}/episodes` — endpoint đó không có ảnh, sắp cũ→mới; đánh đổi: `/videos/episodes` chỉ phủ tập có video/promo, anime rất dài như One Piece có thể thiếu vài chục tập đầu quá cũ) |
| Đề xuất | `GET /anime/{id}/recommendations` |
| Reviews | `GET /anime/{id}/reviews?page=` |
| Ảnh | `GET /anime/{id}/pictures` |
| Thể loại | `GET /genres/anime` |
| Random | `GET /random/anime` |

## Cấu trúc response

Mọi response bọc trong `data`; list có thêm `pagination`:

```json
{
  "data": [ ... ],
  "pagination": {
    "last_visible_page": 20,
    "has_next_page": true,
    "current_page": 1,
    "items": { "count": 25, "total": 500, "per_page": 25 }
  }
}
```

Lỗi trả về: `{ "status": 404, "type": "BadResponseException", "message": "..." }`

## Quy ước DTO (data/remote/dto)

- DTO dùng `kotlinx.serialization`, đặt tên `XxxDto`, field `@SerialName("snake_case")`
- **Mọi field nullable** trừ `mal_id` — dữ liệu scrape từ MAL thường thiếu (score, synopsis, images có thể null)
- Ảnh: dùng `images.jpg.large_image_url`, fallback `image_url`
- Wrapper chung: `JikanResponse<T>(data: T)`, `JikanListResponse<T>(data: List<T>, pagination: PaginationDto)`
- Mapper DTO → domain model đặt tại `domain/mapper`, xử lý null tại đây (score null → hiển thị "N/A")

## Error handling

Repository trả `Result`/sealed class, phân loại:

| Lỗi | Xử lý |
|---|---|
| 429 | Retry tự động (interceptor); nếu vẫn fail → "Quá nhiều yêu cầu, thử lại sau" |
| 404 | "Không tìm thấy" |
| 500/503 | "Máy chủ Jikan đang bận" + nút thử lại |
| IOException | "Kiểm tra kết nối mạng" + fallback cache nếu có |

## Caching — Room + stale-while-revalidate

Jikan KHÔNG hỗ trợ HTTP cache phía client (`Cache-Control: must-revalidate, private`,
`pragma: no-cache`, `expires: -1` — verify 2026-07) → không dùng được OkHttp Cache/ETag.
Cache tự quản trong Room là tầng duy nhất. Server Jikan tự cache ~24h nên TTL client
24h = khớp chu kỳ dữ liệu thực, không phải "chấp nhận dữ liệu cũ".

**Luồng chuẩn (mọi repository có cache):**
1. UI luôn đọc từ Room qua `Flow` — không bao giờ render thẳng từ response API
2. Emit cache ngay (nếu có) → so `fetchedAt` với TTL → hết hạn thì gọi API nền →
   ghi đè vào Room theo key (transaction: xóa list cũ theo `listKey` + insert mới) →
   `Flow` tự re-emit. Không merge — server là nguồn chân lý
3. Refresh lỗi: giữ cache cũ hiển thị + báo lỗi nhẹ (khớp bảng error handling ở trên)
4. Pull-to-refresh bỏ qua TTL — user chủ động là gọi API luôn (vẫn ghi vào Room)

**TTL theo loại dữ liệu:**

| Dữ liệu | TTL |
|---|---|
| `genres/anime` | 7 ngày (gần như tĩnh) |
| seasons/now, top, upcoming | 24h |
| detail full (`/anime/{id}/full`) | 24h |
| Characters, Recommendations, Pictures (Detail) | 7 ngày, Room SWR (gần như tĩnh) |
| Reviews preview (Detail, page 1) | 24h (user đăng liên tục) |
| Episodes preview (Detail, "Các tập") | KHÔNG cache — luôn gọi lại vì tập mới có thể ra bất cứ lúc nào |
| Search | KHÔNG cache (trừ Paging RemoteMediator sau này) |

## Test nhanh endpoint

```bash
curl -s "https://api.jikan.moe/v4/seasons/now?limit=3" | python3 -m json.tool | head -50
```
