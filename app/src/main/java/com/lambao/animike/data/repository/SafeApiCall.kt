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
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // Bắt cả lỗi deserialize (SerializationException...) — Jikan trả dữ liệu
    // thiếu/sai định dạng không nên làm crash toàn app.
    ApiResult.Error(e.toAppError())
}

/**
 * Public để nơi khác không đi qua safeApiCall (VD PagingSource — LoadState.Error
 * chỉ giữ Throwable thô) vẫn phân loại lỗi theo đúng 1 quy tắc duy nhất.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is HttpException -> when (code()) {
        429 -> AppError.RateLimited
        404 -> AppError.NotFound
        500, 503 -> AppError.ServerBusy
        else -> AppError.Unknown(message())
    }

    is IOException -> AppError.NoConnection
    else -> AppError.Unknown(message)
}
