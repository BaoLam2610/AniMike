package com.lambao.animike.data.remote

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.data.remote.dto.CharacterEntryDto
import com.lambao.animike.data.remote.dto.EpisodeDto
import com.lambao.animike.data.remote.dto.GenreDto
import com.lambao.animike.data.remote.dto.ImagesDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import com.lambao.animike.data.remote.dto.JikanResponse
import com.lambao.animike.data.remote.dto.RecommendationEntryDto
import com.lambao.animike.data.remote.dto.ReviewDto
import com.lambao.animike.data.remote.dto.SeasonYearDto
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
}
