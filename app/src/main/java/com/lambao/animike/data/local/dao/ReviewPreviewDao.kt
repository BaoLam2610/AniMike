package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewPreviewDao {
    @Query("SELECT * FROM cached_review_preview WHERE malId = :malId ORDER BY position")
    fun observe(malId: Int): Flow<List<CachedReviewPreviewEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_review_preview WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedReviewPreviewEntity>)

    @Query("DELETE FROM cached_review_preview WHERE malId = :malId")
    suspend fun clear(malId: Int)

    @Transaction
    suspend fun replace(malId: Int, items: List<CachedReviewPreviewEntity>) {
        clear(malId)
        insertAll(items)
    }
}
