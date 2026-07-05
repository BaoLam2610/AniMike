package com.lambao.animike.data.local.entity

import androidx.room.Entity

/** Cache cho gallery ảnh (/pictures) ở Detail — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_picture", primaryKeys = ["malId", "url"])
data class CachedPictureEntity(
    val malId: Int,
    val url: String,
    val position: Int,
    val fetchedAt: Long,
)
