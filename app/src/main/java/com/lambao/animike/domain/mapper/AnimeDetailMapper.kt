package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.RelationGroup
import java.util.Locale

fun AnimeFullDto.toDomain(): AnimeDetail = AnimeDetail(
    malId = malId,
    title = titleEnglish ?: title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    trailerYoutubeId = trailer?.youtubeId,
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
