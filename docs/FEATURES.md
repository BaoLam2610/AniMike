# AniMike — Danh sách tính năng

Ứng dụng Android xem thông tin anime (manga ở phase sau), dữ liệu từ [Jikan API v4](https://docs.api.jikan.moe/).

**Đặc điểm quan trọng của Jikan API:**
- Miễn phí, không cần API key / đăng nhập
- **Chỉ đọc** — không thể cập nhật list lên MyAnimeList → mọi tính năng "lưu/theo dõi" phải lưu local (Room)
- Rate limit: **3 request/giây, 60 request/phút** → bắt buộc có cache + hạn chế gọi song song
- Dữ liệu server đã cache ~24h, không cần refresh liên tục

---

## 1. Tính năng MVP (bắt buộc cho v1)

### 1.1. Trang chủ (Home)
| Tính năng | Endpoint |
|---|---|
| Anime mùa này (Season Now) | `GET /seasons/now` |
| Top anime (theo score/popularity) | `GET /top/anime` |
| Anime sắp chiếu | `GET /seasons/upcoming` |
| Lịch chiếu theo thứ trong tuần | `GET /schedules?filter={day}` |

### 1.2. Tìm kiếm & lọc
| Tính năng | Endpoint |
|---|---|
| Tìm theo tên, phân trang | `GET /anime?q={query}&page={n}` |
| Lọc: type (TV/Movie/OVA...), status, rating, score, genre | `GET /anime?type=&status=&rating=&min_score=&genres=` |
| Sắp xếp: score, popularity, rank, ngày phát hành | `&order_by=&sort=` |
| Danh sách thể loại (để build UI filter) | `GET /genres/anime` |

### 1.3. Chi tiết anime
| Tính năng | Endpoint |
|---|---|
| Thông tin đầy đủ: ảnh, synopsis, score, rank, studio, số tập, trailer YouTube | `GET /anime/{id}/full` |
| Nhân vật + seiyuu (diễn viên lồng tiếng) | `GET /anime/{id}/characters` |
| Danh sách tập | `GET /anime/{id}/episodes` |
| Anime liên quan (sequel, prequel...) | có sẵn trong `/full` (`relations`) |
| Đề xuất tương tự | `GET /anime/{id}/recommendations` |
| Bộ sưu tập ảnh | `GET /anime/{id}/pictures` |

### 1.4. Yêu thích (local)
- Lưu/bỏ lưu anime yêu thích → Room database
- Màn hình danh sách yêu thích, hoạt động offline

### 1.5. Cơ bản về UX
- Loading / error state + retry (quan trọng vì Jikan hay trả 429/503)
- Pull-to-refresh
- Dark mode (Material 3 dynamic color có sẵn)

---

## 2. Tính năng mở rộng (v1.x)

### 2.1. Kho lưu trữ mùa (Season Archive)
- Duyệt anime theo năm + mùa bất kỳ: `GET /seasons/{year}/{season}`, danh sách năm: `GET /seasons`

### 2.2. Theo dõi tiến độ xem (local tracking)
- Trạng thái: Đang xem / Đã xem / Tạm dừng / Bỏ / Dự định xem
- Đánh dấu tập đã xem, chấm điểm cá nhân
- (Thay thế cho MAL list vì API read-only)

### 2.3. Nhân vật & Người & Studio
| Tính năng | Endpoint |
|---|---|
| Chi tiết nhân vật, anime xuất hiện, seiyuu | `GET /characters/{id}/full` |
| Chi tiết người (seiyuu, đạo diễn, staff) | `GET /people/{id}/full` |
| Staff của một anime (đạo diễn, âm nhạc...) | `GET /anime/{id}/staff` |
| Top nhân vật | `GET /top/characters` |
| Trang studio — bấm tên studio ở Detail | `GET /producers/{id}/full` |

### 2.4. Reviews
- Reviews của anime: `GET /anime/{id}/reviews`
- Reviews mới nhất toàn hệ thống: `GET /reviews/anime`

### 2.5. Khám phá
- Random anime ("Hôm nay xem gì?"): `GET /random/anime`
- Tập mới phát hành: `GET /watch/episodes/popular`
- Trailer/promo mới: `GET /watch/promos`
- Đề xuất từ cộng đồng: `GET /recommendations/anime`

### 2.6. Chi tiết anime mở rộng (hướng streaming-app, MVP 3-4)
| Tính năng | Endpoint |
|---|---|
| Nút "Xem trên..." — link nền tảng hợp pháp (Crunchyroll, Netflix...) | `GET /anime/{id}/streaming` |
| Tab media: trailer, PV, promo video | `GET /anime/{id}/videos` |
| Biểu đồ phân bố điểm, số người đang xem/đã xem | `GET /anime/{id}/statistics` |
| Nhạc opening/ending | `GET /anime/{id}/themes` |

---

## 3. Tính năng nâng cao (v2+)

### 3.1. Manga
- Toàn bộ tương tự anime: search `GET /manga`, chi tiết `GET /manga/{id}/full`, top `GET /top/manga`, characters, reviews, recommendations
- Tracking đọc local (chapter/volume)

### 3.2. Xem profile MAL công khai
- `GET /users/{username}/full`, anime list công khai của user
- Chỉ xem, không sửa (API không hỗ trợ auth)

### 3.3. Thông báo lịch chiếu
- Chọn anime đang theo dõi → notification khi có tập mới (WorkManager + `broadcast` field trong data)

### 3.4. Khác
- Deep link: mở app từ link `myanimelist.net/anime/{id}`
- Widget màn hình chính: lịch chiếu hôm nay
- Export/import dữ liệu local (JSON)
- Đa ngôn ngữ (EN/VI), title ưu tiên English/Japanese theo setting
- Offline mode đầy đủ (cache toàn bộ đã xem qua)

---

## 4. Ngoài phạm vi (không làm được với Jikan)
- Đăng nhập MyAnimeList, đồng bộ list lên MAL (cần MAL official API + OAuth — có thể cân nhắc rất xa sau này)
- Xem video anime (Jikan chỉ có metadata, không có nguồn phát)
