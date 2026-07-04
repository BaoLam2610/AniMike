package com.lambao.animike.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.FavoriteDao
import com.lambao.animike.data.local.dao.GenreDao
import com.lambao.animike.data.local.entity.CachedAnimeDetailEntity
import com.lambao.animike.data.local.entity.CachedAnimeListEntity
import com.lambao.animike.data.local.entity.CachedGenreEntity
import com.lambao.animike.data.local.entity.FavoriteEntity

@Database(
    entities = [
        CachedAnimeListEntity::class,
        CachedAnimeDetailEntity::class,
        CachedGenreEntity::class,
        FavoriteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeListDao(): AnimeListDao
    abstract fun animeDetailDao(): AnimeDetailDao
    abstract fun genreDao(): GenreDao
    abstract fun favoriteDao(): FavoriteDao
}
