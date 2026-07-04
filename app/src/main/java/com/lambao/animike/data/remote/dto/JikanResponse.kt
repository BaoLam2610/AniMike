package com.lambao.animike.data.remote.dto

import kotlinx.serialization.Serializable

/** Wrapper cho response trả về 1 object (VD /anime/{id}/full). */
@Serializable
data class JikanResponse<T>(val data: T)
