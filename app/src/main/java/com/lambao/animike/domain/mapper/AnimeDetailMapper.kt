package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.data.remote.dto.TrailerDto
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.RelationGroup
import java.util.Locale

// Khớp id trong ".../embed/{id}?..." (embed_url) và ".../watch?v={id}" (url).
// Id YouTube thực tế luôn 11 ký tự [A-Za-z0-9_-] nhưng nới xuống 5+ cho an toàn.
private val youtubeIdRegex = Regex("""(?:embed/|v=)([A-Za-z0-9_-]{5,})""")

// youtube_id đôi khi null dù trailer tồn tại (VD anime 38524 chỉ có embed_url)
// — rút id từ embed_url/url làm fallback để nút trailer không biến mất oan.
private fun TrailerDto.resolveYoutubeId(): String? =
    youtubeId ?: listOfNotNull(embedUrl, url)
        .firstNotNullOfOrNull { youtubeIdRegex.find(it)?.groupValues?.get(1) }

fun AnimeFullDto.toDomain(): AnimeDetail = AnimeDetail(
    malId = malId,
    title = titleEnglish ?: title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    trailerYoutubeId = trailer?.resolveYoutubeId(),
    score = score?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
    rank = rank?.let { "#$it" } ?: "N/A",
    type = type,
    episodes = episodes,
    year = year,
    status = status ?: "N/A",
    isAiring = airing ?: false,
    studios = studios.mapNotNull { it.name }.joinToString(", ").ifBlank { "N/A" },
    genres = genres.mapNotNull { it.name },
    synopsis = synopsis ?: "Chưa có mô tả.",
    relations = relations.mapNotNull { rel ->
        val animeTitles = rel.entry.filter { it.type == "anime" }.mapNotNull { it.name }
        if (animeTitles.isEmpty()) null else RelationGroup(relation = rel.relation ?: "", titles = animeTitles)
    },
)
