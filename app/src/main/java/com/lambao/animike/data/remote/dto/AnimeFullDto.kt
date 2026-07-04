package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeFullDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    @SerialName("title_english") val titleEnglish: String? = null,
    val images: ImagesDto? = null,
    val trailer: TrailerDto? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val airing: Boolean? = null,
    val duration: String? = null,
    val score: Double? = null,
    val rank: Int? = null,
    val year: Int? = null,
    val synopsis: String? = null,
    val studios: List<NamedResourceDto> = emptyList(),
    val genres: List<NamedResourceDto> = emptyList(),
    val relations: List<RelationDto> = emptyList(),
)

@Serializable
data class TrailerDto(
    @SerialName("youtube_id") val youtubeId: String? = null,
)

@Serializable
data class NamedResourceDto(
    val name: String? = null,
)

@Serializable
data class RelationDto(
    val relation: String? = null,
    val entry: List<RelationEntryDto> = emptyList(),
)

@Serializable
data class RelationEntryDto(
    val type: String? = null,
    val name: String? = null,
)
