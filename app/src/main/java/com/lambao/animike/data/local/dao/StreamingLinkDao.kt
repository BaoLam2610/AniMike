package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedStreamingLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingLinkDao {
    @Query("SELECT * FROM cached_streaming_link WHERE malId = :malId ORDER BY position")
    fun observe(malId: Int): Flow<List<CachedStreamingLinkEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_streaming_link WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedStreamingLinkEntity>)

    @Query("DELETE FROM cached_streaming_link WHERE malId = :malId")
    suspend fun clear(malId: Int)

    @Transaction
    suspend fun replace(malId: Int, items: List<CachedStreamingLinkEntity>) {
        clear(malId)
        insertAll(items)
    }
}
