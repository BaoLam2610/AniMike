package com.lambao.animike.data.remote

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.data.remote.dto.CharacterEntryDto
import com.lambao.animike.data.remote.dto.EpisodeDto
import com.lambao.animike.data.remote.dto.GenreDto
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

    // Chỉ lấy page 1 (100 tập/trang) — đủ cho đa số anime, MVP chưa cần phân
    // trang tập (xem AnimeDetailRepository.getEpisodes).
    @GET("anime/{id}/episodes")
    suspend fun getEpisodes(@Path("id") id: Int, @Query("page") page: Int = 1): JikanListResponse<EpisodeDto>

    @GET("anime/{id}/reviews")
    suspend fun getReviews(@Path("id") id: Int, @Query("page") page: Int = 1): JikanListResponse<ReviewDto>

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("genres") genres: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
    ): JikanListResponse<AnimeDto>

    @GET("genres/anime")
    suspend fun getGenres(): JikanListResponse<GenreDto>

    @GET("schedules")
    suspend fun getSchedules(
        @Query("filter") day: String,
        @Query("page") page: Int = 1,
    ): JikanListResponse<AnimeDto>
}
