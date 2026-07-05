package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.RecommendationPairDto
import com.lambao.animike.domain.model.CommunityRecommendation

// null khi thiếu malId hoặc không đủ đúng 2 entry — cấu trúc coi là hỏng,
// bỏ qua item này thay vì hiển thị cặp thiếu 1 vế (đã verify qua curl luôn
// có 2, nhưng phòng Jikan trả thiếu ở vài response lẻ).
fun RecommendationPairDto.toDomain(): CommunityRecommendation? {
    val id = malId ?: return null
    val first = entry.getOrNull(0) ?: return null
    val second = entry.getOrNull(1) ?: return null
    return CommunityRecommendation(
        id = id,
        firstAnimeId = first.malId,
        firstAnimeTitle = first.title ?: "Không rõ tên",
        firstAnimeImageUrl = first.images?.jpg?.largeImageUrl ?: first.images?.jpg?.imageUrl,
        secondAnimeId = second.malId,
        secondAnimeTitle = second.title ?: "Không rõ tên",
        secondAnimeImageUrl = second.images?.jpg?.largeImageUrl ?: second.images?.jpg?.imageUrl,
        content = content ?: "",
        username = user?.username ?: "Ẩn danh",
    )
}
