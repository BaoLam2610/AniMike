package com.lambao.animike.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retry exponential backoff cho HTTP 429/503, tối đa 3 lần: 1s -> 2s -> 4s
 * (xem .claude/skills/jikan-api). Đặt gần network hơn RateLimitInterceptor
 * để mỗi lần retry cũng được giãn cách đúng 400ms.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 1_000L,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0
        var backoffMs = initialBackoffMs

        while (shouldRetry(response.code) && attempt < maxRetries) {
            response.close()
            Thread.sleep(backoffMs)
            backoffMs *= 2
            attempt++
            response = chain.proceed(request)
        }
        return response
    }

    private fun shouldRetry(code: Int) = code == 429 || code == 503
}
