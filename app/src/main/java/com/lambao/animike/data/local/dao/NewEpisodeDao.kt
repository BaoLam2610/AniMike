package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedNewEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewEpisodeDao {
    @Query("SELECT * FROM cached_new_episode ORDER BY position")
    fun observeAll(): Flow<List<CachedNewEpisodeEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_new_episode")
    suspend fun getFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedNewEpisodeEntity>)

    @Query("DELETE FROM cached_new_episode")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedNewEpisodeEntity>) {
        clear()
        insertAll(items)
    }
}
