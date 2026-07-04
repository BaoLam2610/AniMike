package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lambao.animike.data.local.entity.CachedAnimeDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDetailDao {
    @Query("SELECT * FROM cached_anime_detail WHERE malId = :malId")
    fun observe(malId: Int): Flow<CachedAnimeDetailEntity?>

    @Query("SELECT fetchedAt FROM cached_anime_detail WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedAnimeDetailEntity)
}
