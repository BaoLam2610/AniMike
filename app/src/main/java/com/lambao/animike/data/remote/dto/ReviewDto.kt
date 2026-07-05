package com.lambao.animike.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Verify qua curl (anime/1/reviews): tags LUÔN đúng 1 phần tử, 3 giá trị có
// thể có "Recommended"/"Mixed Feelings"/"Not Recommended" — KHÁC giả định ban
// đầu chỉ có 2 loại. reactions/date/user.images đều có sẵn trong response
// thật, trước đây bị bỏ qua khi map DTO.
@Serializable
data class ReviewDto(
    @SerialName("mal_id") val malId: Int,
    val review: String? = null,
    val score: Int? = null,
    val date: String? = null,
    val tags: List<String> = emptyList(),
    val reactions: ReviewReactionsDto? = null,
    val user: ReviewUserDto? = null,
)

@Serializable
data class ReviewUserDto(
    val username: String? = null,
    val images: ImagesDto? = null,
)

@Serializable
data class ReviewReactionsDto(
    val overall: Int? = null,
    val nice: Int? = null,
    @SerialName("love_it") val loveIt: Int? = null,
    val funny: Int? = null,
    val confusing: Int? = null,
    val informative: Int? = null,
    @SerialName("well_written") val wellWritten: Int? = null,
    val creative: Int? = null,
)
