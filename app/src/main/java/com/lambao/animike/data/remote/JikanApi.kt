package com.lambao.animike.data.remote

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.data.remote.dto.AnimeStatisticsDto
import com.lambao.animike.data.remote.dto.AnimeThemesDto
import com.lambao.animike.data.remote.dto.AnimeVideosDto
import com.lambao.animike.data.remote.dto.CharacterEntryDto
import com.lambao.animike.data.remote.dto.EpisodeDto
import com.lambao.animike.data.remote.dto.GenreDto
import com.lambao.animike.data.remote.dto.ImagesDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import com.lambao.animike.data.remote.dto.JikanResponse
import com.lambao.animike.data.remote.dto.RecommendationEntryDto
import com.lambao.animike.data.remote.dto.RecommendationPairDto
import com.lambao.animike.data.remote.dto.ReviewDto
import com.lambao.animike.data.remote.dto.SeasonYearDto
import com.lambao.animike.data.remote.dto.StreamingLinkDto
import com.lambao.animike.data.remote.dto.WatchEpisodeEntryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApi {

    @GET("seasons/now")
    suspend fun getSeasonNow(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>

    @GET("top/anime")
    suspend fun getTopAnime(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>

    @GET("seasons/upcoming")
    suspend fun getUpcoming(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>

    @GET("seasons")
    suspend fun getSeasonsList(): JikanListResponse<SeasonYearDto>

    @GET("seasons/{year}/{season}")
    suspend fun getSeasonArchive(
        @Path("year") year: Int,
        @Path("season") season: String,
        @Query("page") page: Int = 1,
    ): JikanListResponse<AnimeDto>

    @GET("anime/{id}/full")
    suspend fun getAnimeFull(@Path("id") id: Int): JikanResponse<AnimeFullDto>

    // MVP4 "Hôm nay xem gì?" — trả về 1 anime full ngẫu nhiên, chỉ cần
    // malId để điều hướng sang Detail (đã có sẵn cache/SWR riêng của Detail).
    @GET("random/anime")
    suspend fun getRandomAnime(): JikanResponse<AnimeFullDto>

    // MVP4 "Tập mới phát hành" (kit Animax "New Episode Releases"). Dùng
    // /watch/episodes/popular thay vì /watch/episodes (bản thường) — verify
    // qua curl: cùng model response (entry/episodes/region_locked), nhưng trả
    // các bộ nổi tiếng hơn (Cowboy Bebop, Naruto...) thay vì phim bất kỳ.
    // Không truyền page: đã verify qua curl page=1 và page=2 trả về Y HỆT nhau
    // (endpoint này không thực sự phân trang, luôn trả đúng 1 snapshot cố định).
    @GET("watch/episodes/popular")
    suspend fun getNewEpisodeReleases(): JikanListResponse<WatchEpisodeEntryDto>

    @GET("anime/{id}/characters")
    suspend fun getCharacters(@Path("id") id: Int): JikanListResponse<CharacterEntryDto>

    @GET("anime/{id}/recommendations")
    suspend fun getRecommendations(@Path("id") id: Int): JikanListResponse<RecommendationEntryDto>

    // /videos/episodes (KHÔNG phải /episodes) — có thumbnail + trả sẵn thứ tự
    // mới nhất trước, đúng thứ tự muốn hiển thị (xem comment ở EpisodeDto).
    // Dùng ở 2 nơi: AnimeDetailRepository.getEpisodes() gọi 1 lần page=1 cho
    // preview trong Detail (KHÔNG cache — luôn gọi lại, xem comment ở
    // AnimeDetailRepository); AnimeEpisodesPagingSource gọi phân trang đầy đủ
    // cho EpisodesScreen ("Xem tất cả") — 2 request page=1 riêng biệt khi mở
    // "Xem tất cả" từ Detail (chấp nhận được vì dữ liệu vốn không cache).
    @GET("anime/{id}/videos/episodes")
    suspend fun getEpisodes(@Path("id") id: Int, @Query("page") page: Int = 1): JikanListResponse<EpisodeDto>

    @GET("anime/{id}/reviews")
    suspend fun getReviews(@Path("id") id: Int, @Query("page") page: Int = 1): JikanListResponse<ReviewDto>

    // Mỗi item chỉ là {jpg, webp} — trùng đúng shape ImagesDto, không cần DTO
    // riêng. Không phân trang (đã verify qua curl, ~10-50 ảnh/anime).
    @GET("anime/{id}/pictures")
    suspend fun getPictures(@Path("id") id: Int): JikanListResponse<ImagesDto>

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("genres") genres: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): JikanListResponse<AnimeDto>

    @GET("genres/anime")
    suspend fun getGenres(): JikanListResponse<GenreDto>

    @GET("schedules")
    suspend fun getSchedules(
        @Query("filter") day: String,
        @Query("page") page: Int = 1,
    ): JikanListResponse<AnimeDto>

    // MVP4 "Đề xuất cộng đồng" — feed toàn cục (không theo malId riêng), phân
    // trang THẬT (100 item/trang) — verify qua curl: page=1/page=2 trả data
    // khác nhau (khác /watch/episodes* vốn luôn 1 snapshot cố định).
    @GET("recommendations/anime")
    suspend fun getCommunityRecommendations(@Query("page") page: Int = 1): JikanListResponse<RecommendationPairDto>

    // MVP4 "Biểu đồ phân bố điểm + số người xem" — 1 object/anime (không phân
    // trang), verify qua curl: scores LUÔN đúng 10 phần tử (score 1-10).
    @GET("anime/{id}/statistics")
    suspend fun getAnimeStatistics(@Path("id") id: Int): JikanResponse<AnimeStatisticsDto>

    // MVP4 "Nhạc OP/ED" — 1 object/anime, mỗi opening/ending là 1 chuỗi đã
    // format sẵn từ Jikan (tên bài + nghệ sĩ + khoảng tập).
    @GET("anime/{id}/themes")
    suspend fun getAnimeThemes(@Path("id") id: Int): JikanResponse<AnimeThemesDto>

    // MVP4 nút "Xem trên..." — link các nền tảng streaming hợp pháp
    // (Crunchyroll/Netflix...), danh sách ngắn không phân trang.
    @GET("anime/{id}/streaming")
    suspend fun getStreamingLinks(@Path("id") id: Int): JikanListResponse<StreamingLinkDto>

    // MVP4 tab "Video" (promo/PV + music video) — data là 1 OBJECT (không
    // phải list) nên dùng JikanResponse; mảng episodes bên trong CỐ Ý bỏ qua
    // (xem AnimeVideosDto).
    @GET("anime/{id}/videos")
    suspend fun getAnimeVideos(@Path("id") id: Int): JikanResponse<AnimeVideosDto>
}
