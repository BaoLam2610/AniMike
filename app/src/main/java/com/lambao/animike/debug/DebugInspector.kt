package com.lambao.animike.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

// MVP-Debug Đợt 1 — kho thu thập log trong RAM cho màn Debug (FAB nổi, chỉ
// DEBUG build). Là `object` thuần (KHÔNG Hilt) vì cả DebugNetworkInterceptor
// (tạo trong NetworkModule, không qua DI graph có sẵn) lẫn AppLog (global,
// gọi từ mọi nơi kể cả code không có DI) đều phải ghi vào đây — object global
// tránh phải luồn dependency qua DI. Ring-buffer cap cứng để không phình RAM.
//
// Thread-safety: interceptor chạy trên thread của OkHttp dispatcher, AppLog có
// thể gọi từ thread bất kỳ — MutableStateFlow.update{} là atomic (CAS) nên
// append an toàn không cần lock riêng.
object DebugInspector {

    // Giữ tối đa 200 bản ghi mỗi loại — đủ để soi 1 phiên thao tác, không đủ
    // lớn để dồn RAM. Bản ghi cũ nhất bị đẩy ra khi vượt ngưỡng (takeLast).
    private const val MAX_ENTRIES = 200

    // Mặc định cắt body ở 8KB — đủ soi hình dạng response, không đủ dồn RAM.
    // Runtime-configurable (KHÔNG còn const) qua tab API của màn Debug — user
    // tự tăng khi cần xem trọn response dài, không phải sửa code + rebuild
    // (góp ý user). Đơn vị BYTE (peekBody đếm byte, không phải ký tự).
    const val DEFAULT_BODY_LIMIT_BYTES = 8_000L
    const val UNLIMITED_BODY_BYTES = Long.MAX_VALUE

    private val _bodyLimitBytes = MutableStateFlow(DEFAULT_BODY_LIMIT_BYTES)
    val bodyLimitBytes: StateFlow<Long> = _bodyLimitBytes.asStateFlow()

    // Chỉ áp dụng cho request ghi SAU thời điểm gọi — log đã ghi trước đó vẫn
    // giữ nguyên body đã cắt ở mức cũ (không thể hồi tố, response đã đọc xong).
    fun setBodyLimitBytes(bytes: Long) {
        _bodyLimitBytes.value = bytes
    }

    private val idSeq = AtomicLong(0L)

    private val _networkLogs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    val networkLogs: StateFlow<List<NetworkLogEntry>> = _networkLogs.asStateFlow()

    private val _appLogs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val appLogs: StateFlow<List<AppLogEntry>> = _appLogs.asStateFlow()

    // Mới nhất ở ĐẦU danh sách (UI hiện dòng vừa xảy ra trên cùng) — thêm vào
    // đầu rồi cắt đuôi để giữ đúng MAX_ENTRIES bản gần nhất.
    fun addNetworkLog(entry: NetworkLogEntry) {
        _networkLogs.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    fun addAppLog(entry: AppLogEntry) {
        _appLogs.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    fun clearNetworkLogs() = _networkLogs.update { emptyList() }

    fun clearAppLogs() = _appLogs.update { emptyList() }

    fun nextId(): Long = idSeq.incrementAndGet()
}

data class NetworkLogEntry(
    val id: Long,
    val timestampMs: Long,
    val method: String,
    val url: String,
    // null khi request lỗi ở tầng vận chuyển (không có response) — khi đó `error`
    // được điền. code trong 200..299 = thành công.
    val code: Int?,
    val durationMs: Long,
    // Headers giữ dạng List<Pair> (không Map) để giữ đúng thứ tự + cho phép
    // trùng key (VD Set-Cookie) — dùng ở màn chi tiết.
    val requestHeaders: List<Pair<String, String>>,
    val responseHeaders: List<Pair<String, String>>,
    val requestBody: String?,
    val responseBody: String?,
    val error: String?,
) {
    val isSuccess: Boolean get() = code in 200..299
}

enum class AppLogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

data class AppLogEntry(
    val id: Long,
    val timestampMs: Long,
    val level: AppLogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String?,
)
