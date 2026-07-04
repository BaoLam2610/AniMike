package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedGenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Query("SELECT * FROM cached_genre ORDER BY name")
    fun observeAll(): Flow<List<CachedGenreEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_genre")
    suspend fun getFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedGenreEntity>)

    @Query("DELETE FROM cached_genre")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<CachedGenreEntity>) {
        clearAll()
        insertAll(items)
    }
}
