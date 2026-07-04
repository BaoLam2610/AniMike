package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedSeasonYearEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonYearDao {
    @Query("SELECT * FROM cached_season_year ORDER BY year DESC")
    fun observeAll(): Flow<List<CachedSeasonYearEntity>>

    // MIN đúng vì replaceAll() ghi cả batch trong 1 transaction, mọi row chia
    // sẻ chung 1 fetchedAt — MIN/MAX/bất kỳ row nào cũng cho cùng kết quả.
    @Query("SELECT MIN(fetchedAt) FROM cached_season_year")
    suspend fun getFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedSeasonYearEntity>)

    @Query("DELETE FROM cached_season_year")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<CachedSeasonYearEntity>) {
        clearAll()
        insertAll(items)
    }
}
