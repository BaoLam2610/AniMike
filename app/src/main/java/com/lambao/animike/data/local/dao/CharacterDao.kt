package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedCharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM cached_character WHERE malId = :malId ORDER BY position")
    fun observe(malId: Int): Flow<List<CachedCharacterEntity>>

    @Query("SELECT MIN(fetchedAt) FROM cached_character WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCharacterEntity>)

    @Query("DELETE FROM cached_character WHERE malId = :malId")
    suspend fun clear(malId: Int)

    @Transaction
    suspend fun replace(malId: Int, items: List<CachedCharacterEntity>) {
        clear(malId)
        insertAll(items)
    }
}
