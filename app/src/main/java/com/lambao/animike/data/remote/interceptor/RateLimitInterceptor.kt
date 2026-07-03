package com.lambao.animike.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Jikan cho phép tối đa 3 req/s & 60 req/phút. Giãn cách tối thiểu 400ms giữa
 * 2 request để không bao giờ chạm rate limit (xem .claude/skills/jikan-api).
 */
class RateLimitInterceptor(
    private val minIntervalMs: Long = 400L,
) : Interceptor {

    private val lock = Any()
    private var lastRequestAtMs = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(lock) {
            val elapsed = System.currentTimeMillis() - lastRequestAtMs
            if (elapsed < minIntervalMs) {
                Thread.sleep(minIntervalMs - elapsed)
            }
            lastRequestAtMs = System.currentTimeMillis()
        }
        return chain.proceed(chain.request())
    }
}
