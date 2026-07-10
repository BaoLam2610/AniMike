package com.lambao.animike.domain.mapper

import com.lambao.animike.data.local.entity.CachedAnimeDetailEntity
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.RelationGroup
import com.lambao.animike.domain.model.Studio

// Delimiter ASCII hiếm gặp trong tên anime/thể loại thật — đủ an toàn cho dữ
// liệu cache chỉ dùng để hiển thị (không cần TypeConverter/JSON cho vài chuỗi này).
private const val ITEM_DELIMITER = "~~~"
private const val GROUP_DELIMITER = "###"
private const val FIELD_DELIMITER = ":::"

fun CachedAnimeDetailEntity.toDomain(): AnimeDetail = AnimeDetail(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    trailerYoutubeId = trailerYoutubeId,
    score = score,
    rank = rank,
    type = type,
    episodes = episodes,
    year = year,
    status = status,
    isAiring = isAiring,
    studios = decodeStudios(studiosEncoded),
    genres = decodeList(genresEncoded),
    synopsis = synopsis,
    relations = decodeRelations(relationsEncoded),
)

fun AnimeDetail.toEntity(fetchedAt: Long): CachedAnimeDetailEntity = CachedAnimeDetailEntity(
    malId = malId,
    title = title,
    imageUrl = imageUrl,
    trailerYoutubeId = trailerYoutubeId,
    score = score,
    rank = rank,
    type = type,
    episodes = episodes,
    year = year,
    status = status,
    isAiring = isAiring,
    studiosEncoded = encodeStudios(studios),
    genresEncoded = encodeList(genres),
    synopsis = synopsis,
    relationsEncoded = encodeRelations(relations),
    fetchedAt = fetchedAt,
)

private fun encodeList(items: List<String>): String = items.joinToString(ITEM_DELIMITER)

private fun decodeList(encoded: String): List<String> =
    if (encoded.isEmpty()) emptyList() else encoded.split(ITEM_DELIMITER)

// Studio(malId,name): mỗi studio "malId:::name" nối bằng ITEM_DELIMITER —
// cùng kỹ thuật FIELD_DELIMITER của relations. Bỏ qua entry hỏng format
// (thiếu id/không parse được) khi decode để không crash màn Detail.
private fun encodeStudios(studios: List<Studio>): String =
    studios.joinToString(ITEM_DELIMITER) { "${it.malId}$FIELD_DELIMITER${it.name}" }

private fun decodeStudios(encoded: String): List<Studio> {
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(ITEM_DELIMITER).mapNotNull { studioStr ->
        val parts = studioStr.split(FIELD_DELIMITER, limit = 2)
        if (parts.size < 2) return@mapNotNull null
        val id = parts[0].toIntOrNull() ?: return@mapNotNull null
        Studio(malId = id, name = parts[1])
    }
}

private fun encodeRelations(relations: List<RelationGroup>): String =
    relations.joinToString(GROUP_DELIMITER) { group ->
        "${group.relation}$FIELD_DELIMITER${encodeList(group.titles)}"
    }

private fun decodeRelations(encoded: String): List<RelationGroup> {
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(GROUP_DELIMITER).mapNotNull { groupStr ->
        // Phòng thủ: chỉ đúng format khi encodeRelations() luôn ghi kèm
        // FIELD_DELIMITER (đang đúng vì AnimeDetailMapper lọc bỏ group rỗng
        // trước khi tới đây) — nhưng không dựa hẳn vào invariant ở file khác,
        // tránh crash màn Detail nếu logic đó đổi mà quên cập nhật ở đây.
        val parts = groupStr.split(FIELD_DELIMITER, limit = 2)
        if (parts.size < 2) return@mapNotNull null
        RelationGroup(relation = parts[0], titles = decodeList(parts[1]))
    }
}
