package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.ProducerFullDto
import com.lambao.animike.domain.model.StudioDetail
import com.lambao.animike.domain.model.StudioExternalLink

fun ProducerFullDto.toDomain(): StudioDetail = StudioDetail(
    malId = malId,
    // Không có top-level `name` (verify) — ưu tiên title type "Default",
    // fallback title đầu tiên bất kỳ.
    name = titles.firstOrNull { it.type == "Default" }?.title
        ?: titles.firstNotNullOfOrNull { it.title }
        ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    // `established` ISO date (VD "1979-05-01T00:00:00+00:00") — chỉ hiển thị
    // NĂM nên cắt 4 ký tự đầu, chỉ nhận khi đúng 4 chữ số (phòng format lạ).
    establishedYear = established?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) },
    animeCount = count ?: 0,
    favorites = favorites ?: 0,
    about = about,
    externalLinks = external.mapNotNull { link ->
        val name = link.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val url = link.url?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return@mapNotNull null
        StudioExternalLink(name = name, url = url)
    },
)
