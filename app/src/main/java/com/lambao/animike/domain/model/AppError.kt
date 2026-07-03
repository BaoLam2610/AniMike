package com.lambao.animike.domain.model

/**
 * Phân loại lỗi networking theo quy ước .claude/skills/jikan-api — mỗi biến
 * thể map sang thông báo tiếng Việt hiển thị cho người dùng.
 */
sealed interface AppError {
    data object RateLimited : AppError
    data object NotFound : AppError
    data object ServerBusy : AppError
    data object NoConnection : AppError
    data class Unknown(val rawMessage: String?) : AppError
}

fun AppError.toUserMessage(): String = when (this) {
    AppError.RateLimited -> "Quá nhiều yêu cầu, thử lại sau"
    AppError.NotFound -> "Không tìm thấy"
    AppError.ServerBusy -> "Máy chủ Jikan đang bận"
    AppError.NoConnection -> "Kiểm tra kết nối mạng"
    is AppError.Unknown -> rawMessage ?: "Đã có lỗi xảy ra"
}
