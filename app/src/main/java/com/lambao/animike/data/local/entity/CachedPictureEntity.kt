package com.lambao.animike.data.local.entity

import androidx.room.Entity

// thumbnailUrl (image_url — medium) cho grid preview, fullUrl (large_image_url
// ưu tiên) cho viewer full-screen — TÁCH 2 field thay vì 1 url dùng chung
// (theo yêu cầu user: viewer phải hiện ảnh nét nhất) để grid không phải tải
// ảnh độ phân giải lớn chỉ để hiển thị thumbnail 2:3 nhỏ.
/** Cache cho gallery ảnh (/pictures) ở Detail — docs/ROADMAP.md mục 3b. */
@Entity(tableName = "cached_picture", primaryKeys = ["malId", "fullUrl"])
data class CachedPictureEntity(
    val malId: Int,
    val thumbnailUrl: String,
    val fullUrl: String,
    val position: Int,
    val fetchedAt: Long,
)
