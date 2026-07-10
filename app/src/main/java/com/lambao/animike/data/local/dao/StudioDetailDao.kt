package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lambao.animike.data.local.entity.CachedStudioDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDetailDao {
    @Query("SELECT * FROM cached_studio_detail WHERE studioId = :studioId")
    fun observe(studioId: Int): Flow<CachedStudioDetailEntity?>

    @Query("SELECT fetchedAt FROM cached_studio_detail WHERE studioId = :studioId")
    suspend fun getFetchedAt(studioId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedStudioDetailEntity)
}
