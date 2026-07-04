package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    val filler: Boolean = false,
    val recap: Boolean = false,
)
