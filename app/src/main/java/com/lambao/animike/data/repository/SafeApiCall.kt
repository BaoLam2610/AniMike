package com.lambao.animike.data.repository

import com.lambao.animike.domain.model.AppError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Bọc 1 lệnh gọi Jikan, phân loại lỗi theo bảng trong .claude/skills/jikan-api
 * (429/404/500-503/IOException/deserialize) — dùng chung cho mọi repository.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(call())
} catch (e: HttpException) {
    ApiResult.Error(e.toAppError())
} catch (e: IOException) {
    ApiResult.Error(AppError.NoConnection)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // Bắt cả lỗi deserialize (SerializationException...) — Jikan trả dữ liệu
    // thiếu/sai định dạng không nên làm crash toàn app.
    ApiResult.Error(AppError.Unknown(e.message))
}

private fun HttpException.toAppError(): AppError = when (code()) {
    429 -> AppError.RateLimited
    404 -> AppError.NotFound
    500, 503 -> AppError.ServerBusy
    else -> AppError.Unknown(message())
}
