package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedCommunityRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityRecommendationDao {
    @Query("SELECT * FROM cached_community_recommendation ORDER BY position")
    fun observeAll(): Flow<List<CachedCommunityRecommendationEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_community_recommendation")
    suspend fun getFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCommunityRecommendationEntity>)

    @Query("DELETE FROM cached_community_recommendation")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedCommunityRecommendationEntity>) {
        clear()
        insertAll(items)
    }
}
