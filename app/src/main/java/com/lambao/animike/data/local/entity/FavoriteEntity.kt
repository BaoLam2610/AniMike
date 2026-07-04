package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: String,
    val year: Int?,
    val addedAt: Long,
)
