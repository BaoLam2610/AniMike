package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lambao.animike.data.local.entity.CachedAnimeStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeStatisticsDao {
    @Query("SELECT * FROM cached_anime_statistics WHERE malId = :malId")
    fun observe(malId: Int): Flow<CachedAnimeStatisticsEntity?>

    @Query("SELECT fetchedAt FROM cached_anime_statistics WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedAnimeStatisticsEntity)
}
