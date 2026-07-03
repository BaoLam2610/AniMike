package com.lambao.animike.data.repository

import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AppError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

interface AnimeRepository {
    suspend fun getTopAnime(): ApiResult<List<Anime>>
}

class AnimeRepositoryImpl @Inject constructor(
    private val api: JikanApi,
) : AnimeRepository {

    override suspend fun getTopAnime(): ApiResult<List<Anime>> = try {
        val response = api.getTopAnime()
        ApiResult.Success(response.data.map { it.toDomain() })
    } catch (e: HttpException) {
        ApiResult.Error(e.toAppError())
    } catch (e: IOException) {
        ApiResult.Error(AppError.NoConnection)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Bắt cả lỗi deserialize (SerializationException...) — Jikan trả dữ
        // liệu thiếu/sai định dạng không nên làm crash toàn app.
        ApiResult.Error(AppError.Unknown(e.message))
    }

    private fun HttpException.toAppError(): AppError = when (code()) {
        429 -> AppError.RateLimited
        404 -> AppError.NotFound
        500, 503 -> AppError.ServerBusy
        else -> AppError.Unknown(message())
    }
}
