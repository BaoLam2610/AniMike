package com.lambao.animike.data.remote.interceptor

import com.lambao.animike.debug.DebugInspector
import com.lambao.animike.debug.NetworkLogEntry
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException

// MVP-Debug Đợt 1 — ghi lại từng lần gọi Jikan vào DebugInspector để tab "API"
// của màn Debug soi được (method/url/code/thời gian/body). CHỈ được thêm vào
// chain khi BuildConfig.DEBUG (xem NetworkModule) nên không tồn tại chi phí ở
// release. Đặt SAU RetryInterceptor trong chain → được gọi 1 lần MỖI lần thử
// lại (thấy được cả các attempt 429 bị retry), và đo đúng thời gian HTTP thật
// của từng attempt (không tính phần sleep của RateLimitInterceptor ở tầng
// ngoài).
class DebugNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Đọc 1 lần/request (không phải const) — user có thể đổi runtime qua
        // tab API của màn Debug (xem DebugInspector.setBodyLimitBytes).
        val bodyLimitBytes = DebugInspector.bodyLimitBytes.value
        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Lỗi tầng vận chuyển (mất mạng, timeout...) — ghi entry lỗi rồi
            // ném lại để chain xử lý như bình thường, không nuốt.
            DebugInspector.addNetworkLog(
                NetworkLogEntry(
                    id = DebugInspector.nextId(),
                    timestampMs = System.currentTimeMillis(),
                    method = request.method,
                    url = request.url.toString(),
                    code = null,
                    durationMs = (System.nanoTime() - startNs) / 1_000_000,
                    requestHeaders = request.headers.toPairs(),
                    responseHeaders = emptyList(),
                    requestBody = request.readBodySafely(bodyLimitBytes),
                    responseBody = null,
                    error = e.toString(),
                ),
            )
            throw e
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        // peekBody: đọc BẢN SAO body (tối đa bodyLimitBytes byte) mà KHÔNG tiêu
        // thụ stream gốc — response vẫn dùng được bình thường ở tầng trên
        // (khác response.body.string() sẽ đóng stream). Long.MAX_VALUE (chọn
        // "Không giới hạn") vẫn an toàn — peekBody chỉ đọc tới hết body thật
        // có, không đọc vượt quá dữ liệu tồn tại.
        val responseBody = runCatching {
            response.peekBody(bodyLimitBytes).string()
        }.getOrNull()

        DebugInspector.addNetworkLog(
            NetworkLogEntry(
                id = DebugInspector.nextId(),
                timestampMs = System.currentTimeMillis(),
                method = request.method,
                url = request.url.toString(),
                code = response.code,
                durationMs = durationMs,
                requestHeaders = request.headers.toPairs(),
                responseHeaders = response.headers.toPairs(),
                requestBody = request.readBodySafely(bodyLimitBytes),
                responseBody = responseBody,
                error = null,
            ),
        )
        return response
    }
}

private fun Headers.toPairs(): List<Pair<String, String>> = map { it.first to it.second }

// Jikan đọc-only nên hầu hết request là GET không body — best-effort đọc, nuốt
// mọi lỗi (body có thể one-shot/không đọc lại được), cắt độ dài như response.
// take(Int) nên coerce Long limit về Int — request body thực tế của Jikan
// không bao giờ chạm trần Int.MAX_VALUE.
private fun okhttp3.Request.readBodySafely(limitBytes: Long): String? = runCatching {
    val body = body ?: return null
    val buffer = Buffer()
    body.writeTo(buffer)
    buffer.readUtf8().take(limitBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
}.getOrNull()
