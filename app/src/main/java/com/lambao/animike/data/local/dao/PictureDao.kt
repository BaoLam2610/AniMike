package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedPictureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PictureDao {
    @Query("SELECT * FROM cached_picture WHERE malId = :malId ORDER BY position")
    fun observe(malId: Int): Flow<List<CachedPictureEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_picture WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedPictureEntity>)

    @Query("DELETE FROM cached_picture WHERE malId = :malId")
    suspend fun clear(malId: Int)

    @Transaction
    suspend fun replace(malId: Int, items: List<CachedPictureEntity>) {
        clear(malId)
        insertAll(items)
    }
}
