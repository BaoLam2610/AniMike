package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedTopCharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopCharacterDao {
    @Query("SELECT * FROM cached_top_character ORDER BY position")
    fun observeAll(): Flow<List<CachedTopCharacterEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_top_character")
    suspend fun getFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedTopCharacterEntity>)

    @Query("DELETE FROM cached_top_character")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedTopCharacterEntity>) {
        clear()
        insertAll(items)
    }
}
