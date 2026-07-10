package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// MVP5 Studio Detail — chỉ field scalar của studio, giống Character/Person
// Detail. Danh sách anime studio sản xuất KHÔNG nằm ở đây (gọi riêng qua
// searchAnime(producers=id), Paging 3 — xem StudioDetailRepository).
@Immutable
data class StudioDetail(
    val malId: Int,
    val name: String,
    val imageUrl: String?,
    // Năm thành lập rút gọn từ `established` ISO date (VD "1979") — null nếu
    // Jikan không trả hoặc parse fail (mapper xử lý).
    val establishedYear: String?,
    val animeCount: Int,
    val favorites: Int,
    val about: String?,
    val externalLinks: List<StudioExternalLink>,
)

@Immutable
data class StudioExternalLink(
    val name: String,
    val url: String,
)
