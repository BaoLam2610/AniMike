package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.StreamingLinkDto
import com.lambao.animike.domain.model.StreamingLink

// null khi thiếu url (không có gì để mở) — name thiếu thì fallback hiển thị
// domain thô vẫn hơn là vứt cả link. Chỉ nhận http/https: url này đưa thẳng
// vào ACTION_VIEW (DetailEffect.OpenExternalUrl, KHÔNG sanitize như OpenYoutube)
// — 1 scheme lạ từ API (VD intent://) có thể trigger app khác ngoài ý muốn.
fun StreamingLinkDto.toDomain(): StreamingLink? {
    val url = url?.takeIf { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
        ?: return null
    return StreamingLink(name = name?.takeIf { it.isNotBlank() } ?: url, url = url)
}
