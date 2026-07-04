package com.lambao.animike.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_genre")
data class CachedGenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val fetchedAt: Long,
)
