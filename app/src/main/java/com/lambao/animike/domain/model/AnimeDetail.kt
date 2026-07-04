package com.lambao.animike.domain.model

data class AnimeDetail(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val trailerYoutubeId: String?,
    val score: String,
    val rank: String,
    val type: String?,
    val episodes: Int?,
    val year: Int?,
    val status: String,
    val isAiring: Boolean,
    val studios: String,
    val genres: List<String>,
    val synopsis: String,
    val relations: List<RelationGroup>,
)

data class RelationGroup(
    val relation: String,
    val titles: List<String>,
)
