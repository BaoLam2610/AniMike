package com.lambao.animike.data.remote

import com.lambao.animike.data.remote.dto.AnimeDto
import com.lambao.animike.data.remote.dto.JikanListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {

    @GET("seasons/now")
    suspend fun getSeasonNow(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>

    @GET("top/anime")
    suspend fun getTopAnime(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>

    @GET("seasons/upcoming")
    suspend fun getUpcoming(@Query("page") page: Int = 1): JikanListResponse<AnimeDto>
}
