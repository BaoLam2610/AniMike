package com.lambao.animike.ui.debug

import androidx.compose.runtime.Immutable
import com.lambao.animike.debug.NetworkLogEntry

// MVP-Debug Đợt 2 — màn chi tiết 1 request (mở từ tab API). entry đọc từ
// ring-buffer RAM theo id; resolved=true sau lần collect đầu để phân biệt
// "chưa tra xong" với "đã tra nhưng không thấy" (bị đẩy khỏi buffer / vừa xoá).
@Immutable
data class DebugNetworkDetailState(
    val entry: NetworkLogEntry? = null,
    val resolved: Boolean = false,
)

// Copy body/cURL xử lý ngay ở màn (clipboard là side-effect nền tảng) nên
// Event/Effect để trống — vẫn kế thừa BaseViewModel cho nhất quán kiến trúc.
sealed interface DebugNetworkDetailEvent

sealed interface DebugNetworkDetailEffect
