package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedAnimeListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeListDao {
    @Query("SELECT * FROM cached_anime_list WHERE listKey = :listKey ORDER BY position")
    fun observeList(listKey: String): Flow<List<CachedAnimeListEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_anime_list WHERE listKey = :listKey")
    suspend fun getFetchedAt(listKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAnimeListEntity>)

    @Query("DELETE FROM cached_anime_list WHERE listKey = :listKey")
    suspend fun clearList(listKey: String)

    @Transaction
    suspend fun replaceList(listKey: String, items: List<CachedAnimeListEntity>) {
        clearList(listKey)
        insertAll(items)
    }
}
