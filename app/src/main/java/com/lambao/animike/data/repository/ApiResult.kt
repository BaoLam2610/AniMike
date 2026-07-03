package com.lambao.animike.data.repository

import com.lambao.animike.domain.model.AppError

/**
 * Contract trả về của mọi repository — thay Result<T> để mang theo AppError
 * đã phân loại thay vì Throwable thô (xem .claude/skills/jikan-api).
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val error: AppError) : ApiResult<Nothing>
}
