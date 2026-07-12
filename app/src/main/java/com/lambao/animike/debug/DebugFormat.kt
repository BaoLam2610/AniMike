package com.lambao.animike.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// MVP-Debug Đợt 2 — tiện ích format cho màn chi tiết API log.

// Json riêng cho DEBUG (KHÔNG dùng chung provideJson của NetworkModule để
// khỏi bật prettyPrint cho luồng parse thật). parseToJsonElement rồi encode
// lại: khử escape thừa (\" , \/) mà server nén vào 1 dòng, cho ra JSON thụt
// lề dễ đọc. isLenient để nuốt vài body gần-JSON.
private val prettyJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}

// Body JSON → thụt lề đẹp; không parse được (không phải JSON) → trả nguyên
// bản. Không bao giờ ném — màn chi tiết luôn hiện được gì đó.
fun prettyJsonOrRaw(raw: String): String = runCatching {
    prettyJson.encodeToString(JsonElement.serializer(), prettyJson.parseToJsonElement(raw))
}.getOrDefault(raw)

// Tái tạo lệnh cURL từ 1 request đã ghi — để copy chạy lại/chia sẻ khi debug.
fun buildCurl(entry: NetworkLogEntry): String = buildString {
    append("curl -X ").append(entry.method)
    entry.requestHeaders.forEach { (key, value) ->
        append(" \\\n  -H '").append(key).append(": ").append(value).append("'")
    }
    entry.requestBody?.let { body ->
        append(" \\\n  --data '").append(body).append("'")
    }
    append(" \\\n  '").append(entry.url).append("'")
}
